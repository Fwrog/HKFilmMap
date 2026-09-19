let lang = new URLSearchParams(location.search).get('lang') === 'zh' ? 'zh' : 'en';
function translate() {
  document.documentElement.lang = lang === 'zh' ? 'zh-CN' : 'en';
  document.querySelectorAll('[data-locale]').forEach(node => {node.hidden = node.dataset.locale !== lang;});
  document.getElementById('project-language').textContent = lang === 'zh' ? 'English' : '中文';
}
document.getElementById('project-language').onclick = () => {
  lang = lang === 'en' ? 'zh' : 'en';
  const url = new URL(location.href);url.searchParams.set('lang',lang);history.replaceState(null,'',url);
  translate();
};
translate();
