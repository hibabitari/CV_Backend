# Hikari Connection Pool Configuration

## Development
- Maximum pool size: 10
- Minimum idle: 5
- Leak detection: 60s

## Production
- Maximum pool size: 50
- Minimum idle: 10
- Leak detection: 60s

## Tuning Guidelines

### Calcul du pool size optimal:
```
pool_size = Tn × (Cm − 1) + 1

Tn = Nombre de threads
Cm = Nombre moyen de connexions simultanées par thread

Exemple:
- 100 threads
- 2 connexions en moyenne par thread
pool_size = 100 × (2 - 1) + 1 = 101 ≈ 50 (arrondi)
```

### Monitoring
Actuator metrics disponibles:
- `hikaricp.connections.active`
- `hikaricp.connections.idle`
- `hikaricp.connections.pending`
- `hikaricp.connections.timeout`

### Alertes recommandées
- Active connections > 80% du max → Scale up
- Timeout count > 0 → Investiguer queries lentes
- Leak detection triggered → Bug dans code