import { useEffect,useRef,type RefObject } from 'react';
import { useFrame,useThree } from '@react-three/fiber';
import { Html } from '@react-three/drei';
import { MathUtils, type Group } from 'three';
import { Block,palette } from './props';
import type { Part,SceneFrame } from '../frame';
type Live={current:Record<string,number>};
function Driver({parts,live,reduced}:{parts:Part[];live:Live;reduced:boolean}) {
 const {invalidate}=useThree();
 useEffect(()=>invalidate(),[parts,invalidate]);
 useFrame((_,dt)=>{let moving=false;const ids=new Set(parts.map(p=>p.id));for(const id in live.current)if(!ids.has(id))delete live.current[id];for(const p of parts){live.current[p.id]=reduced?p.height:MathUtils.damp(live.current[p.id]??0,p.height,2.2,Math.min(dt,.1));if(Math.abs(live.current[p.id]-p.height)>.001)moving=true;}if(moving&&!reduced)invalidate();});return null;
}
function Cabinet({portal,part,position,live,selected,onSelect,onHover}:{portal:RefObject<HTMLDivElement>;part:Part;position:[number,number,number];live:Live;selected:boolean;onSelect:(id:string)=>void;onHover:(id:string|null)=>void}) {
 const fill=useRef<Group>(null);const cap=useRef<Group>(null);
 useFrame(()=>{const h=(live.current[part.id]??0)*.35;if(fill.current){fill.current.scale.y=Math.max(.001,h);fill.current.position.y=.24+h/2;}if(cap.current)cap.current.position.y=.3+h;});
 const color=part.kind==='unknown'?palette.unknown:part.kind==='app'?palette.green:palette.blue;
 return <group position={position} onClick={e=>{e.stopPropagation();onSelect(part.id);}} onPointerOver={e=>{e.stopPropagation();onHover(part.id);}} onPointerOut={()=>onHover(null)}>
   <Block size={[1.25,.22,1.15]} color={selected?'#bed0ff':palette.body}/>
   <group ref={fill}><Block size={[1.03,1,.94]} color={color}/></group>
   <group ref={cap}><Block size={[1.2,.16,1.08]}/></group>
   {selected && <mesh position={[0,3.1,0]} rotation={[0,0,Math.PI]}><coneGeometry args={[.13,.32,12]}/><meshStandardMaterial color={palette.blue}/></mesh>}
   <Html portal={portal} position={[0,-.06,.75]} center style={{pointerEvents:'none'}}><span className={`part-label ${selected?'chosen':''}`}>{part.label.length>22?part.label.slice(0,20)+'…':part.label}</span></Html>
 </group>;
}
export function Archive({portal,frame,reduced,selected,onSelect,onHover}:{portal:RefObject<HTMLDivElement>;frame:SceneFrame;reduced:boolean;selected:string|null;onSelect:(id:string)=>void;onHover:(id:string|null)=>void}) {
 const live=useRef<Record<string,number>>({});const parts=[...frame.appsShelf,...frame.dnsShelf];
 return <group>
  <Driver parts={parts} live={live} reduced={reduced}/>
  <Block size={[13,.45,10]} position={[0,-.5,0]} color={palette.ground}/>
  <Block size={[12,.08,.5]} position={[0,-.22,0]} color={palette.road}/>
  <Block size={[11.8,.18,3.4]} position={[0,-.18,-2.6]}/>
  <Block size={[11.8,.18,3.4]} position={[0,-.18,2.6]}/>
  <Html portal={portal} key="app-shelf-label" position={[-5.5,.1,-.95]} style={{pointerEvents:'none'}}><span className="shelf-label">01 / APP ACTIVITY</span></Html>
  <Html portal={portal} key="dns-shelf-label" position={[-5.5,.1,4.25]} style={{pointerEvents:'none'}}><span className="shelf-label">02 / DNS EVIDENCE</span></Html>
  {frame.appsShelf.map((p,i)=><Cabinet portal={portal} key={p.id} part={p} position={[-4.5+(i%5)*2.2,0,-3.4+Math.floor(i/5)*1.6]} live={live} selected={selected===p.id} onSelect={onSelect} onHover={onHover}/>)}
  {frame.dnsShelf.map((p,i)=><Cabinet portal={portal} key={p.id} part={p} position={[-4.5+(i%5)*2.2,0,1.65+Math.floor(i/5)*1.6]} live={live} selected={selected===p.id} onSelect={onSelect} onHover={onHover}/>)}
 </group>;
}
