---
nav_exclude: true
---
# Audiveris Pages
{: .d-block }

## For the end user

- [HandBook]  
This is the general documentation, meant for the end user.  
It is now available in two versions:
    - A web version (the pages you are reading)
    - A PDF version (downloadable via the button below)

{: .d-inline-block }
new
{: .label .label-yellow }

[Download the PDF version](https://github.com/Audiveris/audiveris/releases/download/{{ site.audiveris_version }}/Audiveris_Handbook.pdf)
{: .btn .text-center }

## For the developer
- [Format of ".omr" files]  
This is the description of the internals of any ``.omr`` Audiveris book file
- [Audiveris Wiki]  
This Wiki gathers various articles about the development and potential evolutions of the Audiveris project.

## All contributors

<ul class="list-style-none">
{% for contributor in site.github.contributors %}
  <li class="d-inline-block mr-1">
     <a href="{{ contributor.html_url }}"><img src="{{ contributor.avatar_url }}" width="32" height="32" alt="{{ contributor.login }}"></a>
  </li>
{% endfor %}
</ul>


[Audiveris Wiki]:           https://github.com/Audiveris/audiveris/wiki
[Format of ".omr" files]:   ./_pages/reference/outputs/omr
[HandBook]:                 ./_pages/handbook
