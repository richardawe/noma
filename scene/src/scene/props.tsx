import { RoundedBox } from '@react-three/drei';
export const palette={body:'#fbfcfe',ground:'#dde3ed',road:'#c3cfdf',blue:'#1f5eff',green:'#398475',unknown:'#8b96a8',ink:'#263951'};
export function Block({position=[0,0,0],size,color=palette.body}:{position?:[number,number,number];size:[number,number,number];color?:string}) {
 return <RoundedBox args={size} radius={Math.min(.1,...size.map(v=>v/3))} smoothness={3} position={position} castShadow receiveShadow><meshStandardMaterial color={color} roughness={.65}/></RoundedBox>;
}
