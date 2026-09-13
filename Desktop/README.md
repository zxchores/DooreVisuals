# DooreVisuals (Desktop)

Fabric-клиент визуалов для Minecraft **1.21.11**. Исходники восстановлены из релиза `v3.31.0`.

## Сборка

Нужны JDK 21+ и интернет (Loom качает Minecraft / Yarn).

```bash
cd Desktop
./gradlew build
```

Готовый мод: `Desktop/build/libs/doorevisuals-3.31.0.jar`.

Версия клиента — `DooreClient.VERSION` и `gradle.properties` (`mod_version`).
