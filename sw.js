// Keeps a copy of the app so it still opens with no internet. Network first, so updates always win.
const C='zone-shell-v1',FILES=['./','index.html','config.js'];
self.addEventListener('install',e=>{self.skipWaiting();e.waitUntil(caches.open(C).then(c=>Promise.all(FILES.map(f=>fetch(f,{cache:'no-store'}).then(r=>r.ok&&c.put(f,r)).catch(()=>0)))))});
self.addEventListener('activate',e=>e.waitUntil(caches.keys().then(ks=>Promise.all(ks.filter(k=>k!==C).map(k=>caches.delete(k)))).then(()=>self.clients.claim())));
self.addEventListener('fetch',e=>{const r=e.request,u=new URL(r.url);
 if(r.method!=='GET'||u.origin!==location.origin||u.pathname.endsWith('/sw.js'))return;
 e.respondWith((async()=>{const c=await caches.open(C);
  try{const net=await Promise.race([fetch(r,{cache:'no-store'}),new Promise((_,j)=>setTimeout(()=>j(0),6000))]);
   if(net&&net.ok)c.put(r.mode==='navigate'?'./':r,net.clone());return net}
  catch(x){const m=await c.match(r.mode==='navigate'?'./':r,{ignoreSearch:true});if(m)return m;throw x}})())});
