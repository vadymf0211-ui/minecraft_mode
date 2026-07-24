# Gravity Gun — Гравитационная пушка

Мод для **Minecraft 1.21 / 1.21.1** на **Fabric**: пушка, которая хватает мобов и блоки, держит их в воздухе перед тобой и швыряет с сокрушительной силой. Вдохновлено одной известной физической пушкой из Half-Life 2.

## Возможности

- **Захват мобов** — зомби, коровы, криперы, лодки, вагонетки, ТНТ и даже выпавшие предметы. Дальность луча — 16 блоков.
- **Захват блоков** — вырывай блок прямо из мира и таскай его с собой (земля, камень, руды, что угодно).
- **Мощный бросок** — запусти удерживаемую цель по направлению взгляда. Брошенные мобы получают урон при ударе о стены и сбивают других мобов, летящие блоки калечат всё на своём пути.
- **Аккуратная установка** — с шифтом цель мягко опускается на место: можно переносить блоки без разрушения и расставлять мобов как фигурки.
- **Конвейерные ленты** — полублок-дорожка с анимированными стрелками: двигает мобов, игроков и предметы, держит груз по центру линии. Редстоун-сигнал ставит ленту на паузу, крадущийся игрок может спокойно стоять, а ПКМ с предметом в руке кладёт его прямо на ленту — как в рамку.
- **Картон** — крафтится из бумаги, стелется на пол как ковёр и служит материалом для коробок. Мокрый картон — просто мусор… пока что.
- **Картонные коробки** — переносное хранилище на 14 слотов (половина шалкера): не теряет содержимое при переноске, как шалкер. Но боится воды: брось коробку в воду, зайди в воду с коробкой в руках или поставь её вплотную к воде — она размокнет, всё содержимое выпадет наружу, а от коробки останется лишь мокрый картон.
- Звуки, частицы и свечение удерживаемой цели прилагаются.

## Управление

| Действие | Результат |
|---|---|
| ПКМ по мобу или блоку | Захватить цель (до 16 блоков) |
| ПКМ, пока цель удерживается | Мощный бросок |
| Шифт + ПКМ | Аккуратно отпустить |

**Что нельзя захватить:** игроков, Дракона Края и Визера, бедрок и другие неразрушаемые блоки, а также блоки с инвентарём (сундуки, печи и т.п.) — ваши алмазы в безопасности.

## Крафт

```
Ж Ж Ж        Ж — железный слиток
Р О А        Р — блок редстоуна
Ж Ж Ж        О — око эндера,  А — алмаз
```

**Конвейерная лента** (6 штук за крафт):

```
К К К        К — кожа
Ж Р Ж        Ж — железный слиток,  Р — редстоун-пыль
```

Лента кладётся стрелками в сторону взгляда. Цепочка лент — готовая транспортная линия; в конце поставь воронку, и лут сам сложится в сундук.

**Картон** (2 штуки за крафт): бумага квадратом 2×2.

**Картонная коробка**: 8 листов картона по кругу, центр пустой.

## Установка

1. Установи [Fabric Loader](https://fabricmc.net/use/) для Minecraft 1.21.1.
2. Скачай [Fabric API](https://modrinth.com/mod/fabric-api) и положи в папку `mods`.
3. Положи `gravity-gun-x.x.x.jar` в ту же папку `mods`.
4. Запусти игру и скрафти пушку (или возьми её из творческого раздела «Инструменты»).

## Сборка из исходников

Нужны **JDK 21** и **Gradle 8.12+**:

```bash
gradle build
# готовый мод: build/libs/gravity-gun-<версия>.jar
```

В репозитории нет ни одного бинарного файла: текстура и иконка мода хранятся как текстовые пиксель-карты в `texturegen/*.px` и растеризуются в PNG Gradle-задачей `generateTextures` прямо во время сборки. Перерисовать текстуру можно, отредактировав `texturegen/design.py` и запустив `python3 texturegen/design.py` (предпросмотр — `texturegen/preview.png`).

---

# Gravity Gun (English)

A **Fabric** mod for **Minecraft 1.21 / 1.21.1**: grab mobs and blocks, hold them mid-air and hurl them with devastating force. Inspired by a certain famous physics gun.

**Controls:** right-click to grab whatever you're looking at (16 block range), right-click again to launch it, sneak + right-click to set it down gently. Launched mobs take crash damage and bowl over anything living on their path; launched blocks hurt whatever they land on.

**Can't grab:** players, the Ender Dragon and the Wither, unbreakable blocks (bedrock) and blocks with inventories (chests, furnaces).

**Conveyor Belts:** crafted from leather + iron + redstone (6 per craft). Half-slab belts move mobs and items along their animated arrows and keep cargo centred; a redstone signal pauses the belt, sneaking players stay put, and right-clicking a belt with any item loads it onto the line.

**Cardboard:** craft from 2×2 paper; lay it down as a carpet or build **cardboard boxes** — portable 14-slot storage (half a shulker) that keeps its contents when broken. Boxes hate water: thrown into water, carried into water in hand, or placed next to water, a box soaks through, spills everything and leaves only wet cardboard behind.

**Crafting (shaped, 3×3):** top and bottom rows — three iron ingots each; middle row — redstone block, eye of ender, diamond.

**Building:** JDK 21 + Gradle 8.12+, then `gradle build` → `build/libs/`. The repo is 100% text: textures are stored as `.px` pixel maps in `texturegen/` and rasterised to PNG by the `generateTextures` Gradle task at build time.

## License

MIT — see [LICENSE](LICENSE).
