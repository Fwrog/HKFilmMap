// Imported files are untrusted. Validate the entire batch before saving any note.
export function validateNotes(value, places) {
  if (!Array.isArray(value)) throw new Error('Expected a notes array');
  const ids=new Set();
  return value.map(note=>{
    if (!note || typeof note.id !== 'string' || !note.id || note.id.length>100 || ids.has(note.id) || !places.has(note.placeId) || typeof note.title !== 'string' || !note.title.trim() || note.title.length>100 || typeof note.body !== 'string' || !note.body.trim() || note.body.length>2000) throw new Error('Invalid note');
    ids.add(note.id);
    return {id:note.id,placeId:note.placeId,title:note.title,body:note.body};
  });
}
