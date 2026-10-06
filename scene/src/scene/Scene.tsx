import { useEffect,useState,type RefObject } from 'react';
import { Canvas,useThree } from '@react-three/fiber';
import { Environment,Lightformer,OrbitControls } from '@react-three/drei';
import { ACESFilmicToneMapping,PerspectiveCamera } from 'three';
import type { SceneFrame } from '../frame';
import { Archive } from './Archive';
function CameraRig({frame}:{frame:SceneFrame}) {
 const {camera,size,invalidate}=useThree();
 useEffect(()=>{const aspect=size.width/Math.max(1,size.height);const portrait=aspect<1.1;const scale=Math.sqrt(Math.max(1,1.35/aspect));const angle=(portrait?44:60)*Math.PI/180;const distance=29*(portrait?Math.min(1.35,scale):1);camera.position.set(distance*Math.sin(angle)*.55,distance*Math.cos(angle),distance*Math.sin(angle)*.84);(camera as PerspectiveCamera).fov=Math.min(45,26*scale);camera.lookAt(0,0,0);camera.updateProjectionMatrix();invalidate();},[camera,size,invalidate]);
 useEffect(()=>{invalidate();},[frame,invalidate]);return null;
}
export default function Scene({portal,frame,reduced,selected,onSelect,onHover}:{portal:RefObject<HTMLDivElement>;frame:SceneFrame;reduced:boolean;selected:string|null;onSelect:(id:string)=>void;onHover:(id:string|null)=>void}) {
 const [lost,setLost]=useState(false);
 if(lost)return <div className="fallback">3D paused because the graphics context was lost. Your evidence remains below.</div>;
 return <Canvas shadows dpr={[1,1.5]} frameloop="demand" camera={{fov:26,position:[15,16,23]}} gl={{antialias:true,toneMapping:ACESFilmicToneMapping,toneMappingExposure:.9}} onCreated={({gl})=>{gl.domElement.addEventListener('webglcontextlost',e=>{e.preventDefault();setLost(true);});}}>
  <color attach="background" args={['#e8edf4']}/>
  <ambientLight intensity={.12}/>
  <directionalLight position={[6,12,8]} intensity={2.7} color="#fff1dc" castShadow shadow-mapSize={[1024,1024]} shadow-camera-left={-10} shadow-camera-right={10} shadow-camera-top={10} shadow-camera-bottom={-10} shadow-bias={-.0004} shadow-normalBias={.04}/>
  <Environment resolution={128} environmentIntensity={.28}><Lightformer intensity={2} position={[0,8,0]} rotation={[Math.PI/2,0,0]} scale={[10,10,1]}/><Lightformer intensity={1} position={[-8,3,0]} rotation={[0,Math.PI/2,0]} scale={[8,5,1]}/><Lightformer intensity={1} position={[4,4,-6]} scale={[8,4,1]}/></Environment>
  <CameraRig frame={frame}/><Archive portal={portal} frame={frame} reduced={reduced} selected={selected} onSelect={onSelect} onHover={onHover}/>
  <OrbitControls enablePan={false} enableDamping={!reduced} minPolarAngle={.35} maxPolarAngle={1.25} minDistance={22} maxDistance={45} target={[0,0,0]}/>
 </Canvas>;
}
