import {test} from 'node:test';
import assert from 'node:assert/strict';
import {frame,type Snapshot} from '../src/frame';
export const snapshot:Snapshot={date:'2026-10-06',zone:'Europe/London',generatedAt:1791288000000,retainedFrom:1791201600000,partialRetention:false,usage:true,dns:true,status:'Observing standard DNS',error:'',events:[],matches:[{domain:'example.org',organisation:'Example',categories:'',source:'TEST FIXTURE',version:'test'}]};
const event=(i:number,value='example.org',kind='DNS_REQUEST')=>({id:`fixture-${i}`,time:snapshot.generatedAt-100000+i,kind,value,source:'TEST FIXTURE'});
test('reference: ten records have height one, sixty have height six',()=>{for(const n of [0,1,10,60,70,20000]){const f=frame({...snapshot,events:Array.from({length:n},(_,i)=>event(i))});assert.equal(f.requests,n);if(n)assert.equal(f.dnsShelf[0].height,Math.min(6,n/10));for(const p of f.dnsShelf){assert.ok(Number.isFinite(p.height));assert.ok(p.height>=0&&p.height<=6);}}});
test('dedup, expiry, future and invalid times are excluded; unknown stays unknown',()=>{const f=frame({...snapshot,events:[event(1),event(1),event(2,'unknown.test'),{...event(3),time:0},{...event(4),time:Infinity},{...event(5),time:snapshot.generatedAt+1}]});assert.equal(f.requests,2);assert.equal(f.owners,1);assert.equal(f.dnsShelf[1].kind,'unknown');assert.deepEqual(f.dnsShelf[1].evidenceIds,['fixture-2']);});
test('app and DNS shelves never attribute ownership; overflow preserves all records',()=>{const events=Array.from({length:20},(_,i)=>event(i,`app-${i}`,'APP_RESUMED'));const f=frame({...snapshot,events:[...events,event(30)]});assert.equal(f.appsShelf.length,9);assert.equal(f.appsShelf.flatMap(p=>p.evidenceIds).length,20);assert.equal(f.apps,20);assert.equal(f.requests,1);assert.equal(f.dnsShelf[0].evidenceIds.length,1);});
test('chapter replay is cumulative; empty gaps are not invented; erasure removes objects',()=>{const events=[{...event(1),time:snapshot.generatedAt-3600000},event(2)];const s={...snapshot,events};const f=frame(s);assert.equal(f.chapters.length,2);assert.equal(frame(s,f.chapters[0]).requests,1);assert.equal(frame(snapshot).dnsShelf.length,0);});
test('all four health states derive from real collector inputs',()=>{const states=[frame(snapshot),frame({...snapshot,dns:false}),frame({...snapshot,partialRetention:true}),frame({...snapshot,error:'Local data could not be read.'})];assert.deepEqual(states.map(s=>s.health),['good','ok','warning','failing']);console.log('Reachable health states:',states.map(s=>({health:s.health,reason:s.reason})));});
test('read failure cannot display stale records, and owner overflow preserves counts',()=>{
 const events=Array.from({length:12},(_,i)=>event(i,`host${i}.example`));
 const matches=events.map((e,i)=>({domain:e.value,organisation:`Owner ${i}`,categories:'',source:'fixture',version:'1'}));
 const f=frame({...snapshot,events,matches});assert.equal(f.dnsShelf.length,9);assert.equal(f.dnsShelf.reduce((sum,p)=>sum+p.count,0),12);assert.equal(f.owners,12);
 const failed=frame({...snapshot,events,matches,error:'Unreadable'});assert.equal(failed.requests,0);assert.equal(failed.dnsShelf.length,0);
});
