#!/usr/bin/env python3
"""Explicit build-time download. Never runs inside the Android app."""
import hashlib,json,subprocess
from pathlib import Path
REV='06c65bd1e7f214bca9553fb2430d2eae040276b2'
ROOT=f'https://raw.githubusercontent.com/duckduckgo/tracker-radar/{REV}/'
def download(url):
    print('Reading '+url,flush=True)
    return subprocess.check_output(['curl','-sSL','--fail','--connect-timeout','15','--max-time','60','--retry','2',url])
records=[]
sources=[]
for domain in ('facebook.com','google-analytics.com','doubleclick.net','amazon-adsystem.com'):
    url=ROOT+'domains/US/'+domain+'.json'
    raw=download(url)
    entry=json.loads(raw)
    assert entry['domain']==domain
    sources.append({'url':url,'sha256':hashlib.sha256(raw).hexdigest()})
    # Expand ONLY subdomains explicitly listed by upstream. No wildcard ownership.
    names=[domain]+[s+'.'+domain for s in entry.get('subdomains',[]) if s]
    for name in names:
        records.append({'domain':name,'organisation':entry['owner']['name'],
                        'categories':', '.join(entry.get('categories',[])), 'source':url})
obj={'version':REV,'retrieved':'2026-09-22','license':'CC-BY-NC-SA-4.0',
     'notice':'Derived from DuckDuckGo Tracker Radar. Noncommercial evaluation only; source names preserved.',
     'sources':sources,'records':records}
out=Path(__file__).resolve().parents[1]/'app/src/main/assets'
raw=(json.dumps(obj,indent=2)+'\n').encode()
(out/'registry.json').write_bytes(raw)
(out/'registry.sha256').write_text(hashlib.sha256(raw).hexdigest()+'\n')
(out/'TRACKER_LICENSE.txt').write_bytes(download(ROOT+'LICENSE'))
print(f'Imported {len(records)} exact records at {REV}')
