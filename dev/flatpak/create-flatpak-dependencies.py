#! /usr/bin/python3
# Inspired by
# https://stackoverflow.com/questions/28436473/build-gradle-repository-for-offline-development

import os
import glob
import shutil
import subprocess
import sys
from functools import total_ordering
from urllib import request, error as urlerror
from hashlib import sha1

APP_ID = "org.audiveris.audiveris"
project_dir = os.path.dirname(os.path.realpath(__file__))

@total_ordering
class Artifact:

    def __init__(self, group_id, artifact_id, version_id, item_name, sha1):
        self.group_id = group_id.replace(".", "/")
        self.artifact_id = artifact_id
        self.version_id = version_id
        self.item_name = item_name
        self.sha1 = "0" * (40 - len(sha1)) + sha1


    def dir(self):
        return "/".join([self.group_id, self.artifact_id, self.version_id])

    def path(self):
        return "/".join([self.dir(), self.item_name])

    def url(self):
        if self.artifact_id.startswith("com.springsource.javax.media.jai"):
            host = "https://repository.springsource.com/maven/bundles/external"
        elif self.artifact_id in ("jai-core", "jai-codec"):
            host = "https://repository.jboss.org/nexus/content/repositories/thirdparty-releases"
        else:
            host = "https://repo1.maven.org/maven2"
        return "/".join([host, self.path()])

    def yml(self, indent=6):
        spc = indent * " "
        return f"""\
{spc}- type: file
{spc}  url: {self.url()}
{spc}  sha1: {self.sha1}
"""

    def script(self, dest="dependencies"):
        dir = "/".join([dest, self.dir()])
        return f"""\
mkdir -p {dir}
ln -f {self.item_name} {"/".join([dir, self.item_name])}
"""

    def __eq__(self, other):
        return (self.path(), self.sha1) == (other.path(), other.sha1)

    def __ne__(self, other):
        return not (self == other)

    def __lt__(self, other):
        return (self.path(), self.sha1) < (other.path(), other.sha1)


def main(build_dir):
    artifacts = []
    repo_dir = os.path.join(project_dir, "dependencies")
    temp_home = os.path.join(build_dir, "dev/flatpak/gradle")
    if not os.path.isdir(build_dir):
        raise RuntimeError(f"{build_dir} does not exist")

    os.chdir(build_dir)

    # By default, gradle stores the cached artifacts under $HOME/.gradle
    # We can't uset that because it might contain lots of artifacts that
    # we don't need. Therefore use a different user home (-g)
    # Fixme: how to clean up this temp dir?
    # Fixme: do we need to call the "build" task, really?
    subprocess.call(["./gradlew", "-q", "-g", temp_home, "build"])
    os.chdir(project_dir)

    # Dir layout of file created by gradle: e.g.
    # ${GRADLE_TEMP}/caches/modules-2/files-2.1/org.audiveris/proxymusic/4.0.2/7a747a5b8d1e738e74abf883d9c23b0b18f0bf22/proxymusic-4.0.2.jar

    # Target dir layout:
    # dependencies/org/audiveris/proxymusic/4.0.2

    # cache_files = ${GRADLE_TEMP}/caches/modules-*/files-*/
    cache_files = os.path.join(temp_home, "caches", "modules-*", "files-*")
    for cache_dir in glob.glob(cache_files):
        # cache_dir = ${GRADLE_TEMP}/caches/modules-2/files-2.1/
        for cache_group_id in os.listdir(cache_dir):
            # cache group_id is the "vendor", e.g. "org.audiveris"
            cache_group_dir = os.path.join(cache_dir, cache_group_id)
            # repo_group_dir changes this to maven format
            # repo_group_dir = "dependencies/org/audiveris"
            repo_group_dir = os.path.join(repo_dir, cache_group_id.replace('.', '/'))
            for cache_artifact_id in os.listdir(cache_group_dir):
                # cache_artifact_id = "proxymusic"
                cache_artifact_dir = os.path.join(cache_group_dir, cache_artifact_id)
                repo_artifact_dir = os.path.join(repo_group_dir, cache_artifact_id)
                for cache_version_id in os.listdir(cache_artifact_dir):
                    # cache_version_id = "4.0.2"
                    cache_version_dir = os.path.join(cache_artifact_dir, cache_version_id)
                    # the first glob is for the SHA1, the 2nd for the file name
                    cache_items = os.path.join(cache_version_dir, "*/*")
                    for cache_item in glob.glob(cache_items):
                        # cache_item = ".../4.0.2/7a747a5b8d1e738e74abf883d9c23b0b18f0bf22/proxymusic-4.0.2.jar"
                        # cache_item_name = "proxymusic-4.0.2.jar"
                        cache_item_name = os.path.basename(cache_item)
                        artifact = Artifact(cache_group_id, cache_artifact_id,
                                            cache_version_id, cache_item_name,
                                            os.path.basename(os.path.dirname(cache_item)))
                        artifacts.append(artifact)
                        if cache_item_name.endswith("-linux-x86_64.jar"):
                            url = artifact.url().replace("-linux-x86_64.jar", "-linux-arm64.jar.sha1")
                            try:
                                conn = request.urlopen(url)
                                sha1 = conn.read()
                            except urlerror.HTTPError:
                                print(f"ERROR opening '{url}': {sys.exc_info()[1]}")
                            else:
                                if len(sha1) == 40:
                                    arm = Artifact(cache_group_id, cache_artifact_id,
                                                   cache_version_id,
                                                   cache_item_name.replace("-linux-x86_64.jar", "-linux-arm64.jar"),
                                                   sha1.decode("utf-8"))
                                    artifacts.append(arm)
                                    print(f"Added sha1 for {url}: {sha1.decode('utf-8')}")
                                else:
                                    print(f"ERROR: invalid sha1 for {url}: {sha1.decode('utf-8')}")

    artifacts.sort()

    with open("../flathub/dependencies.yml", "w") as output:
        output.write(f"""
{"".join([a.yml() for a in artifacts])}
""")
    with open("../flathub/mkgradlerepo.sh", "w") as out:
        out.write(f"""\
#! /bin/bash
{"".join([a.script() for a in artifacts])}
""")

if __name__ == "__main__":
    main_dir = os.path.dirname(os.path.dirname(project_dir))
    main(main_dir)
