import { validateNotes } from './notes-model.js';

export function initNotebook({places, available, local, language, selectPlace, onChange}) {
  const $ = id => document.getElementById(id);
  const text = (en,zh) => language() === 'zh' ? zh : en;
  const escape = value => value.replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  let notes = [], editing = null;
  try { notes = validateNotes(JSON.parse(localStorage.getItem('hkfilmmap.notes.v1') || '[]'),available); }
  catch { $('note-status').textContent=text('Saved notes could not be read. Import an exported copy to recover them.','无法读取已保存的笔记，请导入备份恢复。'); }

  function persist(next) {
    try { localStorage.setItem('hkfilmmap.notes.v1',JSON.stringify(next)); }
    catch { throw new Error(text('Could not save. Free browser storage or export your notes before trying again.','无法保存。请释放浏览器空间，或先导出笔记再重试。')); }
    notes=next;
    onChange();
  }
  function render(query='') {
    const needle=query.trim().toLocaleLowerCase();
    const matches=notes.filter(n=>[n.title,n.body,local(places.get(n.placeId),'name')].join(' ').toLocaleLowerCase().includes(needle));
    $('result-count').textContent=matches.length;
    $('results').innerHTML=matches.length ? matches.map(n=>`<article class="note-card"><button class="note-location" data-show-note="${escape(n.id)}">⌖ ${escape(local(places.get(n.placeId),'name'))}</button><h3>${escape(n.title)}</h3><p>${escape(n.body)}</p><div><button data-edit-note="${escape(n.id)}">${text('Edit','编辑')}</button><button data-delete-note="${escape(n.id)}">${text('Delete','删除')}</button></div></article>`).join('') : `<div class="empty"><h3>${text('Your own field notes.','自己的电影漫游笔记。')}</h3><p>${needle ? text('No notes match this search.','没有匹配的笔记。') : text('Save a thought, a scene or a place to revisit. Choose New note to begin.','记录一段场景、一个想法或一个想重访的地点。点击“新建笔记”开始。')}</p></div>`;
  }
  function open(placeId=null,note=null) {
    editing=note?.id ?? null;
    $('note-editor-title').textContent=note ? text('Edit note','编辑笔记') : text('New note','新建笔记');
    $('note-place').innerHTML=[...available].map(id=>`<option value="${id}">${escape(local(places.get(id),'name'))}</option>`).join('');
    $('note-title').value=note?.title || '';
    $('note-text').value=note?.body || '';
    $('note-place').value=note?.placeId || placeId || [...available][0];
    $('note-error').textContent='';
    $('note-dialog').showModal(); $('note-title').focus();
  }
  $('new-note').onclick=()=>open();
  $('note-cancel').onclick=()=>$('note-dialog').close();
  $('note-form').onsubmit=event=>{
    event.preventDefault();
    const title=$('note-title').value.trim(),body=$('note-text').value.trim();
    if(!title || !body){$('note-error').textContent=text('Enter a title and note.','请输入标题和笔记内容。');return;}
    const note={id:editing || crypto.randomUUID(),placeId:Number($('note-place').value),title,body};
    try { persist(editing ? notes.map(n=>n.id===editing?note:n) : [note,...notes]); }
    catch(error){$('note-error').textContent=error.message;return;}
    $('note-dialog').close();$('note-status').textContent=text('Note saved in this browser.','笔记已保存在当前浏览器。');
  };
  $('results').addEventListener('click',event=>{
    const button=event.target.closest('button');if(!button)return;
    const {editNote,deleteNote,showNote}=button.dataset;
    if(editNote)open(null,notes.find(n=>n.id===editNote));
    if(showNote)selectPlace(notes.find(n=>n.id===showNote).placeId);
    if(deleteNote){
      const removed=notes.find(n=>n.id===deleteNote);
      try { persist(notes.filter(n=>n.id!==deleteNote)); }
      catch(error){$('note-status').textContent=error.message;return;}
      $('note-status').innerHTML=`${text('Note deleted.','笔记已删除。')} <button id="undo-note">${text('Undo','撤销')}</button>`;
      $('undo-note').onclick=()=>{try{persist([removed,...notes]);$('note-status').textContent=text('Note restored.','笔记已恢复。');}catch(error){$('note-status').textContent=error.message;}};
    }
  });
  $('export-notes').onclick=()=>{
    const url=URL.createObjectURL(new Blob([JSON.stringify(notes,null,2)],{type:'application/json'}));
    const link=document.createElement('a');link.href=url;link.download='HKFilmMap-notes.json';link.click();URL.revokeObjectURL(url);
    $('note-status').textContent=text('Notes exported. Keep the file to restore them later.','笔记已导出，请保留文件以便恢复。');
  };
  $('import-notes').onchange=async event=>{
    const file=event.target.files[0];if(!file)return;
    try {
      if(file.size>2_000_000)throw new Error('size');
      const imported=validateNotes(JSON.parse(await file.text()),available);
      // Existing notes win on duplicate IDs; importing never overwrites local edits.
      persist([...notes,...imported.filter(n=>!notes.some(existing=>existing.id===n.id))]);
      $('note-status').textContent=text('Import complete. Existing notes were kept.','导入完成，已保留原有笔记。');
    }catch{$('note-status').textContent=text('Import failed. Choose an HKFilmMap notes export under 2 MB and check that browser storage is available.','导入失败。请选择小于 2 MB 的 HKFilmMap 笔记导出文件，并检查浏览器存储空间。');}
    event.target.value='';
  };
  return {render,open};
}
