# raft-consensus

Implementación en Java puro del algoritmo de consenso [Raft](https://raft.github.io/) (Ongaro & Ousterhout, 2014), desarrollada como primera fase de un Trabajo de Fin de Grado sobre la integración de Raft en [Hyperledger Besu](https://github.com/hyperledger/besu).

Este motor se verificó como librería independiente — con procesos y red reales, no una simulación en memoria — antes de reutilizarse sin modificar su lógica interna dentro del prototipo embebido en Besu: [besu-raft-module](https://github.com/Chali1302/besu-raft-module).

## Qué incluye

- **`core/`** — el motor Raft: `RaftNode` (estados, *terms*, elección, replicación), `RaftLog`, `ElectionRules`, `CommitRules`, `ClusterConfig`, `EventLogger`, persistencia (`PersistentStore`/`InMemoryPersistentStore`), planificador de temporizadores (`RaftScheduler`/`ScheduledExecutorRaftScheduler`).
- **`rpc/`** — canal HTTP para `RequestVote`/`AppendEntries` (`HttpRaftTransport`, `RaftRpcServer`, `JsonCodec`).
- **`statemachine/`** — una máquina de estados de ejemplo: almacén clave-valor (`KeyValueStateMachine`) con comandos `SET`/`GET`/`DELETE`.
- **`server/`** — `RaftServerMain`, el punto de entrada para arrancar un nodo.
- **`client/`** — `ClientCli`, un cliente interactivo que redirige automáticamente al líder del clúster.

## Requisitos

- Java 21
- Maven

## Compilar

```bash
mvn package
```

Genera `target/raft-consensus.jar` y copia las dependencias de *runtime* en `target/lib/`.

## Arrancar un clúster

### Opción rápida (Windows, PowerShell)

```powershell
scripts\start-cluster.ps1 -N 3 -ConfigFile cluster-3.conf
```

Arranca 3 nodos en segundo plano (logs en `logs/`, datos en `data/node<N>/`), usando el fichero de configuración `cluster-configs\cluster-3.conf`. Para pararlo:

```powershell
scripts\stop-cluster.ps1 -N 3
```

### Manual (cualquier plataforma)

Por cada nodo, en su propia terminal:

```bash
java -cp "target/classes;target/lib/*" edu.tfg.raft.server.RaftServerMain \
  --id=1 --config=cluster-configs/cluster-3.conf --data-dir=data/node1
```

(cambia `--id` y `--data-dir` para cada nodo; en Linux/macOS usa `:` en vez de `;` en el *classpath*).

### Formato del fichero de clúster

```
# id,host,puerto
1,127.0.0.1,9001
2,127.0.0.1,9002
3,127.0.0.1,9003
```

## Cliente

```bash
java -cp "target/classes;target/lib/*" edu.tfg.raft.client.ClientCli --config=cluster-configs/cluster-3.conf
```

```
Cliente Raft. Comandos: set <k> <v> | get <k> | del <k> | salir
> set foo bar
OK
> get foo
bar
```

## Observabilidad

Cada nodo escribe sus transiciones de estado (`BECAME_CANDIDATE`, `BECAME_LEADER`, `VOTE_GRANTED`...) como líneas JSON en `data/node<N>/events.jsonl`, con precisión de milisegundo — es la fuente de la que salen las mediciones de tiempo de elección/recuperación del TFG.

## Limitaciones deliberadas

- Persistencia solo en memoria (`InMemoryPersistentStore`): un nodo pierde su *term*/voto si el proceso muere.
- Sin gestión dinámica de membresía del clúster (altas/bajas en caliente).

Ambas quedan fuera de alcance a propósito, documentado así en la memoria del TFG.
