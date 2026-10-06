import {readFileSync,readdirSync} from 'node:fs';
import {gzipSync} from 'node:zlib';
const dir=new URL('../../app/src/main/assets/evidence-scene/',import.meta.url);
const manifest=JSON.parse(readFileSync(new URL('.vite/manifest.json',dir)));
const entry=manifest['index.html'];const bytes=gzipSync(readFileSync(new URL(entry.file,dir))).length;
const total=readdirSync(new URL('assets/',dir)).reduce((n,f)=>n+gzipSync(readFileSync(new URL('assets/'+f,dir))).length,0);
console.log(JSON.stringify({nativeMainJsBefore:0,entryGzipBytes:bytes,totalAssetsGzipBytes:total},null,2));
if(bytes>1024)throw Error('Entry exceeds the 1 KB budget');
