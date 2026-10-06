export type RecordEvent = {id:string;time:number;kind:string;value:string;source:string};
export type Match = {domain:string;organisation:string;categories:string;source:string;version:string};
export type Snapshot = {date:string;zone:string;generatedAt:number;retainedFrom:number;partialRetention:boolean;usage:boolean;dns:boolean;status:string;error:string;events:RecordEvent[];matches:Match[]};
export type Health = 'good'|'ok'|'warning'|'failing';
export type Part = {
  /** Event.value, or Classification.organisation; prefixed to separate evidence kinds. */ id:string;
  /** Event.value or Classification.organisation. Never inferred from timing. */ label:string;
  /** Event.kind and exact registry match determine the shelf. */ kind:'app'|'owner'|'unknown'|'overflow';
  /** Number of retained Event IDs represented, including overflow. */ count:number;
  /** count / 10, clamped 0..6; ten records are one reference unit. */ height:number;
  /** Stable Event.id references for the native evidence inspector. */ evidenceIds:string[];
  /** Event.value distinct domain count; zero for app tiles. */ names:number;
};
export type SceneFrame = {
  /** Derived from snapshot.error, dns, status and partialRetention; not a risk score. */ health:Health;
  /** Plain explanation of those same status inputs. */ reason:string;
  /** Distinct Event.value where kind is APP_RESUMED. */ apps:number;
  /** Count of Event.kind === DNS_REQUEST. */ requests:number;
  /** Count of distinct normalized DNS Event.value. */ names:number;
  /** Distinct exact Classification.organisation matches. */ owners:number;
  /** Up to eight app tiles plus one overflow group, derived from Event values. */ appsShelf:Part[];
  /** Up to eight matched organisations plus overflow and Unknown. */ dnsShelf:Part[];
  /** floor(Event.time / 900000) * 900000; only occupied 15-minute bins. */ chapters:number[];
  /** Retained, validated and deduplicated Event records in time order. */ events:RecordEvent[];
};
const normalize=(s:string)=>s.toLowerCase().replace(/\.$/,'');
const kinds=new Set(['DNS_REQUEST','APP_RESUMED','APP_PAUSED']);
export function frame(snapshot:Snapshot, chapter:number|null=null):SceneFrame {
  const all=[...new Map((snapshot.error?[]:snapshot.events).filter(e=>e.id && Number.isFinite(e.time) && e.time>=snapshot.retainedFrom && e.time<=snapshot.generatedAt && kinds.has(e.kind)).map(e=>[e.id,e])).values()].sort((a,b)=>a.time-b.time || a.id.localeCompare(b.id));
  const chapters=[...new Set(all.map(e=>Math.floor(e.time/900000)*900000))];
  const events=chapter===null?all:all.filter(e=>e.time<chapter+900000);
  const matches=new Map(snapshot.matches.map(m=>[normalize(m.domain),m]));
  const make=(id:string,label:string,kind:Part['kind'],records:RecordEvent[]):Part=>({id,label,kind,count:records.length,height:Math.min(6,records.length/10),evidenceIds:records.map(e=>e.id),names:kind==='app'?0:new Set(records.map(e=>normalize(e.value))).size});
  const group=(records:RecordEvent[],key:(e:RecordEvent)=>string)=>{const result=new Map<string,RecordEvent[]>();for(const e of records){const k=key(e);if(!result.has(k))result.set(k,[]);result.get(k)!.push(e);}return [...result.entries()].sort(([a],[b])=>a.localeCompare(b));};
  const cap=(parts:Part[],prefix:string)=>{if(parts.length<=8)return parts;const ids=new Set(parts.slice(8).flatMap(p=>p.evidenceIds));return [...parts.slice(0,8),make(`${prefix}:more`,`${parts.length-8} more ${prefix==='app'?'apps':'organisations'}`,prefix==='app'?'app':'overflow',events.filter(e=>ids.has(e.id)))];};
  const appEvents=events.filter(e=>e.kind==='APP_RESUMED');
  const dnsEvents=events.filter(e=>e.kind==='DNS_REQUEST');
  const owners=group(dnsEvents.filter(e=>matches.has(normalize(e.value))),e=>matches.get(normalize(e.value))!.organisation).map(([name,rs])=>make(`owner:${name}`,name,'owner',rs));
  const unknown=dnsEvents.filter(e=>!matches.has(normalize(e.value)));
  let health:Health='ok',reason='Collection is off. Retained evidence is still available.';
  if(snapshot.dns){health='good';reason='Standard DNS observation is running. Coverage remains partial.';}
  else if(snapshot.usage)reason='App activity is enabled. DNS observation is off.';
  if(snapshot.partialRetention){health='warning';reason='Only part of this date is retained.';}
  if(/unavailable|revoked|network.*(?:lost|changed)/i.test(snapshot.status)){health='warning';reason=snapshot.status;}
  if(snapshot.error || /failed/i.test(snapshot.status)){health='failing';reason=snapshot.error || snapshot.status;}
  return {health,reason,apps:new Set(appEvents.map(e=>e.value)).size,requests:dnsEvents.length,names:new Set(dnsEvents.map(e=>normalize(e.value))).size,owners:owners.length,appsShelf:cap(group(appEvents,e=>e.value).map(([name,rs])=>make(`app:${name}`,name,'app',rs)),'app'),dnsShelf:[...cap(owners,'owner'),...(unknown.length?[make('unknown','Unknown','unknown',unknown)]:[])],chapters,events};
}
