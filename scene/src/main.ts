// No React or WebGL in the entry chunk. Android injects the current snapshot after page load.
import type { Snapshot } from './frame';
declare global { interface Window { nomaUpdate: (data: Snapshot) => void; nomaSnapshot?: Snapshot } }
window.nomaUpdate = data => { window.nomaSnapshot = data; window.dispatchEvent(new Event('noma-snapshot')); };
void import('./mount');
