import {execFileSync} from 'node:child_process';
import {readFileSync,readdirSync,writeFileSync} from 'node:fs';
const tree=JSON.parse(execFileSync('npm',['ls','--omit=dev','--all','--json'],{encoding:'utf8'}));
const names=new Set();function walk(n){for(const [name,dep] of Object.entries(n.dependencies||{})){if(dep.version)names.add(name);walk(dep);}}walk(tree);
let text='Noma evidence scene: third-party software notices\n\nIncludes production dependency notices; not every dependency is emitted by tree-shaking.\n\n';
for(const name of [...names].sort()){
 const dir=new URL('../node_modules/'+name+'/',import.meta.url);
 try{const pkg=JSON.parse(readFileSync(new URL('package.json',dir)));text+=`\n--- ${name} ${pkg.version} (${typeof pkg.license==='string'?pkg.license:'see notice'}) ---\n`;
 const files=readdirSync(dir).filter(f=>/^licen[sc]e(?:\.|$)/i.test(f));for(const f of files)text+='\n'+readFileSync(new URL(f,dir),'utf8')+'\n';
 }catch{throw Error('Could not read licence for '+name);}
}
writeFileSync(new URL('../../app/src/main/assets/SCENE_NOTICES.txt',import.meta.url),text);
