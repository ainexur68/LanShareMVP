from pathlib import Path
import re, sys
root = Path(__file__).resolve().parents[1]
required = [
    'AGENTS.md','README.md','docs/REQUIREMENTS.md','docs/ARCHITECTURE.md','docs/PROTOCOL.md',
    'docs/STATE_MACHINE.md','docs/SECURITY_STORAGE.md','docs/TEST_PLAN.md','docs/ROADMAP.md','docs/AGENT_GUIDE.md',
    'app/src/main/AndroidManifest.xml','app/src/main/java/com/example/lanshare/MainActivity.kt',
    'app/src/main/java/com/example/lanshare/TransferProtocol.kt',
    'app/src/main/java/com/example/lanshare/ReceiveDirectory.kt'
]
missing=[p for p in required if not (root/p).exists()]
if missing:
    print('MISSING', *missing, sep='\n'); sys.exit(1)
manifest=(root/'app/src/main/AndroidManifest.xml').read_text(encoding='utf-8')
code=(root/'app/src/main/java/com/example/lanshare/TransferProtocol.kt').read_text(encoding='utf-8')
service=(root/'app/src/main/java/com/example/lanshare/TransferService.kt').read_text(encoding='utf-8')
discovery=(root/'app/src/main/java/com/example/lanshare/DiscoveryManager.kt').read_text(encoding='utf-8')
main=(root/'app/src/main/java/com/example/lanshare/MainActivity.kt').read_text(encoding='utf-8')
access_token=(root/'app/src/main/java/com/example/lanshare/AccessToken.kt').read_text(encoding='utf-8')
directory=(root/'app/src/main/java/com/example/lanshare/ReceiveDirectory.kt').read_text(encoding='utf-8')
checks={
 'ACTION_SEND':'android.intent.action.SEND' in manifest,
 'ACTION_SEND_MULTIPLE':'android.intent.action.SEND_MULTIPLE' in manifest,
 'foreground dataSync':'foregroundServiceType="dataSync"' in manifest,
 'prepare endpoint':'/v1/prepare' in code,
 'status endpoint':'/v1/status' in code,
 'upload endpoint':'/v1/upload' in code,
 'complete endpoint':'/v1/complete' in code,
 'sha256 mismatch 422':'422' in code and 'sha256 mismatch' in code,
 'Downloads/LanShare':'Environment.DIRECTORY_DOWNLOADS + "/LanShare"' in code,
 'stream buffer':'1024 * 1024' in code,
 'access token':'AccessToken.matches' in code and 'token' in discovery,
 'four digit numeric token':'nextInt(10_000)' in access_token and "padStart(4, '0')" in access_token and 'Regex("[0-9]{4}")' in main,
 'open receive directory':'ACTION_VIEW' in directory and 'ACTION_OPEN_DOCUMENT_TREE' in directory and 'openReceiveDirectory' in main,
 'dynamic service port':'for (offset in 0..10)' in service,
}
for k,v in checks.items(): print(('PASS' if v else 'FAIL'), k)
if not all(checks.values()): sys.exit(2)
print('STATIC_VERIFY_PASS')
