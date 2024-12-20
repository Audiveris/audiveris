---
nav_exclude: true
---
# Audiveris Pages
{: .d-block }

## For the end user

- [HandBook](_pages/handbook)  
General user documentation  
Now available in web and PDF versions
{: .d-inline-block }
new
{: .label .label-yellow }  
[Download PDF file](https://github.com/Audiveris/audiveris/releases/download/{{ site.audiveris_release }}/Audiveris_Handbook.pdf)
{: .btn .text-center }

## For the developer
- [Format of ".omr" files](_pages/outputs/omr)  
Description of the internals of any ".omr" Audiveris book file
- [Audiveris Wiki](https://github.com/Audiveris/audiveris/wiki)  
Articles about the development and potential evolutions of Audiveris project.

## Audiveris contributors
<ul class="list-style-none">
{% for contributor in site.github.contributors %}
  <li class="d-inline-block mr-1">
     <a href="{{ contributor.html_url }}"><img src="{{ contributor.avatar_url }}" width="32" height="32" alt="{{ contributor.login }}"></a>
  </li>
{% endfor %}
</ul>

