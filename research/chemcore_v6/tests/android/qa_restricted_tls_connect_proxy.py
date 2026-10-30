#!/usr/bin/env python3
"""CI-only HTTPS CONNECT transport bridge for Android Emulator on a hosted runner.

Why: an Android emulator's virtual DNS resolver may not reach GitHub even
though the GitHub Actions host has real HTTPS connectivity. This proxy DOES
NOT MITM TLS, modify manifests, replace GitHub sources or skip cert checks.
The emulator still performs TLS certificate verification against GitHub.
Only raw.githubusercontent.com:443 is allowed. The public production app
is never configured to use this test-only transport.
"""
import argparse
import logging
import select
import socket
import socketserver

ALLOWED = ("raw.githubusercontent.com", 443)
BUF_LIMIT=8192
TIMEOUT=45.0

class RestrictedConnect(socketserver.BaseRequestHandler):
    def handle(self):
        remote=None
        client=self.request
        client.settimeout(15)
        try:
            first=b""
            while b"\r\n\r\n" not in first and len(first)<BUF_LIMIT:
                chunk=client.recv(1024)
                if not chunk:return
                first+=chunk
            line=first.split(b"\r\n",1)[0].decode("ascii","replace")
            if line != "CONNECT raw.githubusercontent.com:443 HTTP/1.1":
                logging.warning("Blocked proxy target: %s",line[:250])
                client.sendall(b"HTTP/1.1 403 Forbidden\r\nConnection: close\r\n\r\n")
                return
            remote=socket.create_connection(ALLOWED,timeout=18)
            remote.setblocking(False)
            client.sendall(b"HTTP/1.1 200 Connection Established\r\n\r\n")
            # Additional buffered data may be part of the TLS handshake.
            if b"\r\n\r\n" in first:
                tail=first.split(b"\r\n\r\n",1)[1]
                if tail: remote.sendall(tail)
            client.setblocking(False)
            for _ in range(10000):
                readable,_,_=select.select([client,remote],[],[],TIMEOUT)
                if not readable:break
                for inbound in readable:
                    data=inbound.recv(65536)
                    if not data:return
                    outgoing=remote if inbound is client else client
                    outgoing.sendall(data)
        except (OSError,ValueError) as e:
            logging.warning("N16 proxy closed: %s",type(e).__name__)
        finally:
            if remote is not None:
                try:remote.close()
                except OSError:pass

class Server(socketserver.ThreadingTCPServer):
    allow_reuse_address=True
    daemon_threads=True

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--port",type=int,default=18888)
    a=p.parse_args()
    logging.basicConfig(level=logging.INFO,format="%(asctime)s %(levelname)s %(message)s")
    with Server(("0.0.0.0",a.port),RestrictedConnect) as server:
        logging.info("N16 only raw.githubusercontent.com:443, port=%d",a.port)
        server.serve_forever(poll_interval=.5)

if __name__=="__main__":main()
