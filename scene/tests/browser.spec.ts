import {test,expect} from '@playwright/test';
import {fileURLToPath} from 'node:url';
const shots=fileURLToPath(new URL('../../docs/screenshots/',import.meta.url));
import type {Snapshot} from '../src/frame';
const now=Date.parse('2026-10-06T14:00:00Z');
const fixture:Snapshot={date:'2026-10-06 · TEST FIXTURE',zone:'Europe/London',generatedAt:now,retainedFrom:now-86400000,partialRetention:false,usage:true,dns:true,status:'Observing standard DNS',error:'',matches:[{domain:'alpha.example',organisation:'Atlas · fixture',categories:'',source:'TEST FIXTURE',version:'test'},{domain:'beta.example',organisation:'Northstar · fixture',categories:'',source:'TEST FIXTURE',version:'test'},{domain:'gamma.example',organisation:'Fieldwork · fixture',categories:'',source:'TEST FIXTURE',version:'test'}],events:Array.from({length:94},(_,i)=>({id:`fixture-${i}`,time:now-7200000+i*75000,kind:i%5===0?'APP_RESUMED':'DNS_REQUEST',value:i%5===0?['Browser · fixture','Notes · fixture','Calendar · fixture'][i%3]:['alpha.example','beta.example','gamma.example','unknown.example'][i%4],source:'TEST FIXTURE ONLY'}))};
test('real production page: evidence, replay, updates, failures, erasure, mobile and dark',async({page})=>{
 const errors:string[]=[];page.on('pageerror',e=>errors.push(e.message));
 const outside:string[]=[];page.on('request',r=>{if(!r.url().startsWith('http://127.0.0.1:4173')&&!r.url().startsWith('data:'))outside.push(r.url());});
 await page.goto('/');await page.waitForFunction(()=>typeof window.nomaUpdate==='function');await page.evaluate(data=>window.nomaUpdate(data),fixture);
 await expect(page.locator('canvas')).toBeVisible();await expect(page.getByText('Observing DNS',{exact:true})).toBeVisible();await page.waitForTimeout(3500);
 await expect(page.locator('.shelf-label')).toHaveCount(2);
 await page.screenshot({path:shots+'archive-desktop.png',fullPage:true});
 await page.locator('.object-list button').filter({hasText:'Atlas'}).click();await expect(page.locator('.detail h2')).toContainText('Atlas');await expect(page.locator('#evidence .event-row').first()).toBeVisible();
 await page.screenshot({path:shots+'archive-detail.png',fullPage:true});
 await page.locator('.steps button').first().click();await expect(page.locator('.steps button').first()).toHaveAttribute('aria-pressed','true');await expect(page.locator('.kpi').nth(1).locator('strong')).not.toHaveText('75');
 await page.getByRole('button',{name:'Live',exact:true}).click();await expect(page.locator('.kpi').nth(1).locator('strong')).toHaveText('75');
 await page.evaluate(data=>window.nomaUpdate({...data,error:'Local data could not be read.',events:[],matches:[]}),fixture);await expect(page.getByText('Needs attention',{exact:true})).toBeVisible();await expect(page.locator('.object-list button')).toHaveCount(0);await expect(page.locator('#evidence')).toHaveCount(0);await page.screenshot({path:shots+'archive-failure.png',fullPage:true});
 await page.evaluate(data=>window.nomaUpdate({...data,events:[],matches:[],dns:false,usage:false,status:'DNS observation is off'}),fixture);await expect(page.locator('.kpi').first().locator('strong')).toHaveText('0');
 await page.evaluate(data=>window.nomaUpdate(data),fixture);await page.setViewportSize({width:390,height:844});await page.emulateMedia({reducedMotion:'reduce'});await page.waitForTimeout(1200);expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);await page.screenshot({path:shots+'archive-phone.png',fullPage:true});
 await page.emulateMedia({colorScheme:'dark'});await page.screenshot({path:shots+'archive-dark.png',fullPage:true});
 expect(errors).toEqual([]);expect(outside).toEqual([]);
});
test('no WebGL keeps counts and original evidence usable',async({page})=>{
 await page.addInitScript(()=>{const original=HTMLCanvasElement.prototype.getContext;HTMLCanvasElement.prototype.getContext=function(this:HTMLCanvasElement,type:string,...args:unknown[]){if(type==='webgl'||type==='webgl2'||type==='experimental-webgl')return null;return (original as any).call(this,type,...args);} as any;});
 await page.goto('/');await page.waitForFunction(()=>typeof window.nomaUpdate==='function');await page.evaluate(data=>window.nomaUpdate(data),fixture);await expect(page.getByText('3D is unavailable on this device.',{exact:false})).toBeVisible();await expect(page.locator('.object-list button').first()).toBeVisible();await page.locator('.object-list button').first().click();
 const id=await page.locator('#evidence .event-row').first().textContent();expect(id).toBeTruthy();
 await page.route('https://noma.local/evidence?*',route=>route.fulfill({contentType:'text/plain',body:'Native inspector intercepted'}));await page.locator('#evidence .event-row').first().click();await expect(page).toHaveURL(/https:\/\/noma.local\/evidence\?id=fixture-/);
});
