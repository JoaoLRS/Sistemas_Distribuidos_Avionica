import os
import time
import requests

BACKEND_URL = os.getenv("BACKEND_GATEWAY_URL", "http://localhost:8080")

class CristianClock:
    def __init__(self, backend_url=BACKEND_URL):
        self.backend_url = backend_url
        self.offset_ms = 0
        self.synchronized = False

    def sincronizar(self):
        """
        Executa o Algoritmo de Cristian para sincronizar com o servidor central (Gateway).
        """
        try:
            url = f"{self.backend_url}/api/time-sync"
            t0 = int(time.time() * 1000)  # T0: tempo local antes da requisição (ms)
            response = requests.get(url, timeout=3)
            t1 = int(time.time() * 1000)  # T1: tempo local após o retorno (ms)
            
            if response.status_code == 200:
                data = response.json()
                # Tenta obter server_time ou server_time_ms
                server_time = data.get("server_time") or data.get("server_time_ms")
                
                # Se server_time for em nanossegundos, converte para milissegundos
                if server_time is not None:
                    if server_time > 10000000000000:  # Nanossegundos
                        server_time_ms = server_time // 1000000
                    else:
                        server_time_ms = server_time
                    
                    rtt = t1 - t0
                    # Estima o tempo do servidor corrigido
                    tempo_sincronizado = server_time_ms + (rtt / 2)
                    # Calcula o offset a ser somado ao tempo local atual (t1)
                    self.offset_ms = tempo_sincronizado - t1
                    self.synchronized = True
                    print(f"⏰ [Cristian] Sincronizado com {url}! RTT: {rtt}ms | Offset: {self.offset_ms}ms")
                    return True
        except Exception as e:
            print(f"⚠️ [Cristian] Falha ao sincronizar tempo com {self.backend_url}: {e}")
        return False

    def obter_tempo_ms(self):
        """
        Retorna o tempo sincronizado atual em milissegundos.
        """
        return int(time.time() * 1000) + self.offset_ms

    def obter_tempo_s(self):
        """
        Retorna o tempo sincronizado atual em segundos (formato time.time()).
        """
        return self.obter_tempo_ms() / 1000.0

    def obter_tempo_iso(self):
        """
        Retorna o tempo sincronizado no formato ISO 8601 UTC.
        """
        from datetime import datetime, timezone
        return datetime.fromtimestamp(self.obter_tempo_s(), timezone.utc).isoformat()
