# Folders

This chapter describes the various Audiveris application folders on the target machine,
depending on the operating system.

To allow its use by different users on the same machine, Audiveris always stores information
in _user-specific_ locations.

For such user-specific information, there are further distinctions between:
1. [Standard](/folders/standard.md) information, such as the various score `data` output files,
which are the only files any user can directly work upon.
2. [Essential](/folders/essential.md) information, such as `config` or `train` files,
which impact the application behavior and which should be modified only by an advanced user.
3. [Cached](/folders/cached.md) (non-essential) information, such as `log`, `temp` or `gui` files,
which are not meant to be edited.

These distinctions are enforced as much as possible, using the operating system features.
For a general presentation, you can refer to the
[X Desktop Group](https://en.wikipedia.org/wiki/Freedesktop.org)
and especially the [XDG](https://standards.freedesktop.org/basedir-spec/basedir-spec-latest.html)
base directory specification.
