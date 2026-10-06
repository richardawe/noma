# Local IPv4 forwarding

Noma vendors HevSocks5Tunnel 2.17.1, including its source dependencies, under `vendor/hev-socks5-tunnel`. The release archive was fetched from the official upstream release at https://github.com/heiher/hev-socks5-tunnel/releases/tag/2.17.1 . Archive SHA-256: `a7b86050091c5a268d81de70b95d3bb0871ba4136160662b6496596761c2f9a7`. The archive's `.rev-id` is `9a06bc6`. Source is built locally, not downloaded at app runtime. Upstream implementation is unmodified. Upstream MIT/BSD licences and notices are retained; an additional libyaml 0.2.5 upstream notice was added from https://github.com/yaml/libyaml/blob/0.2.5/License . This does not change the separate Tracker Radar noncommercial restriction.

NDK r27c (27.2.12479018) builds the shared library for arm64-v8a and x86_64. This build supports 64-bit Android devices. Native linker options support 16 KB pages, but a 16 KB-page device has not been tested. The JNI class name is upstream's fixed `hev.htproxy.TProxyService` contract.

The VPN sends IPv4 packets into the native TCP/IP stack. The stack connects to a randomly assigned loopback port, authenticates with a fresh private session password and forwards TCP streams / UDP datagrams to Noma's local Kotlin relay. Every outbound socket is protected from the VPN and bound to the selected underlying network. Noma's own UID is excluded. No remote SOCKS service, hardcoded public resolver, TLS interception, remote account or cloud processing is used.

The relay supports SOCKS CONNECT and upstream's UDP-in-TCP command 0x05. In the pinned implementation the first two UDP frame bytes specify **payload length**, despite the upstream README describing total message length. IPv4 headers are ten bytes. Numeric IPv4 addresses are required; the relay never resolves SOCKS destination names.

Standard port-53 questions are inspected for names; payloads are not stored. Synthetic DNS address 10.111.0.1 is mapped to the underlying network's existing IPv4 DNS resolver. Explicit DNS destinations remain unchanged. TCP DNS uses the upstream TCP service if available. For a UDP-only network proxy such as the emulator DNS proxy, synthetic TCP DNS is bridged to UDP. A truncated upstream answer stays truncated; this fallback cannot manufacture the full answer if the upstream provides no TCP service.

The VPN is explicitly bypassable (`Builder.allowBypass`): apps that choose another network may bypass observation. With non-bypassable policy the API 35 emulator failed automatic async DNS selection, even though explicit-network async queries and raw IPv4 socket preflight succeeded. Bypassable policy passes both forms and the tests assert that each async query produces observed evidence. This is an observation tool, not a VPN privacy/security boundary.

IPv6 bypasses collection. Active Private DNS and IPv6-only DNS resolvers are refused. App-encrypted DNS and cached queries remain invisible. ICMP and other non-TCP/UDP IPv4 protocols are not forwarded. DNS names do not establish originating-app ownership, tracking, a visit or data transfer.

The native engine is capped at 48 sessions. The relay admits at most 48 authenticated or handshaking sessions, with at most four UDP destinations per association. Handshakes/connects time out after 10 seconds, UDP after 60 seconds, and TCP idle reads after five minutes. Over-capacity flows may fail. This is an experimental limit, not a production throughput claim.

Stop, revoke, network loss/change and fatal local failures fence writes, close relays, stop/join the native engine, close TUN and remove its private configuration. Session generations keep old callbacks from stopping a new session. A health check detects unexpected native exit. No VPN restart is scheduled after process death or reboot.
