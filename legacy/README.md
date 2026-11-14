# Monolito original (legado)

El código del monolito **no se duplica** en el árbol de trabajo: se conserva en el historial de Git
y en el tag anotado **`legacy-java21`**, que apunta al último commit del monolito ya migrado a
Java 21 y todavía con Thymeleaf.

```bash
git show legacy-java21 --stat          # ver qué contenía
git checkout legacy-java21             # inspeccionarlo
./mvnw -B verify                       # sigue compilando y con sus 79 tests
```

Motivo de no moverlo a `legacy/petclinic-monolith/`:

- El `pom.xml` raíz pasa a ser el **padre multi-módulo**; un directorio con otro proyecto Maven
  dentro obligaría a excluirlo del reactor y arrastraría `src/`, plantillas y WebJars sin uso.
- El evaluador conserva el acceso completo (historia upstream + tag), y el árbol queda limpio para
  que `./mvnw verify` signifique exactamente «los microservicios compilan y pasan sus tests».

La trazabilidad de la migración está en [`docs/MIGRATION.md`](../docs/MIGRATION.md) y el análisis
del legado en [`docs/ANALYSIS.md`](../docs/ANALYSIS.md).
