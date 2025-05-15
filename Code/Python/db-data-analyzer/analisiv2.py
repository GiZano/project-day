import subprocess
import psycopg2
from datetime import datetime, timedelta
import sys

# Configurazione del database ThingsBoard
DB_CONFIG = {
    'host': 'localhost',
    'database': 'thingsboard',
    'user': 'postgres',
    'password': 'PostGres153?',
    'port': '5432'
}

def get_device_metrics(device_id):
    """Recupera le metriche disponibili per un dispositivo"""
    conn = psycopg2.connect(**DB_CONFIG)
    cursor = conn.cursor()

    query = """
    SELECT DISTINCT key as metric_name
    FROM ts_kv
    WHERE entity_id = %s
    ORDER BY key;
    """

    cursor.execute(query, (device_id,))
    metrics = [row[0] for row in cursor.fetchall()]

    cursor.close()
    conn.close()

    return metrics


def get_metric_data(device_id, metric_name, days=7):
    """Recupera i dati di una specifica metrica"""
    conn = psycopg2.connect(**DB_CONFIG)
    cursor = conn.cursor()

    end_ts = int(datetime.now().timestamp() * 1000)
    start_ts = int((datetime.now() - timedelta(days=days)).timestamp() * 1000)

    query = ("""
    SELECT 
        to_timestamp(ts/1000) as timestamp,
        CASE 
            WHEN bool_v IS NOT NULL THEN bool_v::text
            WHEN str_v IS NOT NULL THEN str_v
            WHEN long_v IS NOT NULL THEN long_v::text
            WHEN dbl_v IS NOT NULL THEN dbl_v::text
        END as value
    FROM ts_kv
    WHERE entity_id = %s
      AND key = %s
      AND ts BETWEEN %s AND %s
    ORDER BY ts DESC;
    """)

    cursor.execute(query, (device_id, metric_name, start_ts, end_ts))
    data = (cursor.fetchall())

    cursor.close()
    conn.close()

    return data


def main():
    device_id = input("\n\nInserisci l'ID del dispositivo: ")

    # Recupera le metriche disponibili
    try:
        metrics = get_device_metrics(device_id)
    except psycopg2.errors.InvalidTextRepresentation:
        print("Valore invalido!!")
        return

    print(f"\nMetriche disponibili per il dispositivo {device_id}:")
    for i, metric in enumerate(metrics, 1):
        print(f"{i}. {metric}")

    metric_choice = int(input("\nSeleziona il numero della metrica da analizzare: ")) - 1
    try:
        selected_metric = metrics[metric_choice]
    except IndexError:
        print("Valore invalido!!")
        return

    # Recupera i dati
    try:
        days = int(input("Inserisci il numero di giorni da analizzare (default 7): ") or 7)
        raw_data = get_metric_data(device_id, selected_metric, days)
    except ValueError:
        print("Valore invalido!!")
        return

    if not raw_data:
        print("Nessun dato trovato per i parametri specificati.")
        return

    # Prepara il riassunto dei dati
    timestamps = [row[0] for row in raw_data]
    values = [float(row[1]) for row in raw_data if row[1].replace('.', '', 1).isdigit()]

    data_summary = {
        'period': f"{timestamps[-1].strftime('%Y-%m-%d')} al {timestamps[0].strftime('%Y-%m-%d')}",
        'sample_count': len(raw_data),
        'min_value': min(values) if values else 'N/A',
        'max_value': max(values) if values else 'N/A',
        'avg_value': sum(values) / len(values) if values else 'N/A'
    }

    print("\nInvio i dati a Mistral per l'analisi...")

    prompt = f"""
        Analizza i seguenti dati IoT della metrica '{selected_metric}' e fornisci un riassunto in italiano:
        - Periodo coperto: {data_summary['period']}
        - Numero di campioni: {data_summary['sample_count']}
        - Valore minimo: {data_summary['min_value']}
        - Valore massimo: {data_summary['max_value']}
        - Valore medio: {data_summary['avg_value']}

        Fornisci un'analisi completa che includa:
        1. Un riassunto delle caratteristiche principali
        2. Eventuali anomalie o pattern interessanti
        3. Consigli per il monitoraggio futuro
        """

    print("\n=== ANALISI MISTRAL ===")

    process = subprocess.Popen(
        [".\\ollama", "run", "mistral:7b", "Rispondi solo all’analisi dei dati forniti"],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        text=True,
        encoding="utf-8",
        bufsize=1
    )

    process.stdin.write(prompt)
    process.stdin.close()

    for line in process.stdout:
        print(line, end = "")


if __name__ == "__main__":
    try:
        main()
    except UnboundLocalError:
        print("Errore!")