# Шаг 5. UI-тесты: Selenium + Cucumber + JUnit 5

> Этот документ объясняет, **что** было сделано на шаге 5, **зачем** и **как это проверить руками**.
> Читать удобнее всего в IntelliJ: откройте файл и включите режим **Preview** (иконка справа вверху).

## Содержание

1. [Общая картина](#1-общая-картина)
2. [Что добавилось в модуль tests](#2-что-добавилось-в-модуль-tests)
3. [Как всё связано: от строчки сценария до клика в браузере](#3-как-всё-связано-от-строчки-сценария-до-клика-в-браузере)
4. [Gherkin: как писать сценарии](#4-gherkin-как-писать-сценарии)
5. [Step definitions: перевод фраз в Java](#5-step-definitions-перевод-фраз-в-java)
6. [Page Object Model](#6-page-object-model)
7. [Ожидания: самое важное в UI-тестах](#7-ожидания-самое-важное-в-ui-тестах)
8. [Браузер: DriverFactory, headless, Selenium Manager](#8-браузер-driverfactory-headless-selenium-manager)
9. [UiContext и PicoContainer: общий браузер для всех шагов](#9-uicontext-и-picocontainer-общий-браузер-для-всех-шагов)
10. [Hooks: подготовка и скриншот при падении](#10-hooks-подготовка-и-скриншот-при-падении)
11. [Быстрая подготовка через API](#11-быстрая-подготовка-через-api)
12. [Раннер и теги](#12-раннер-и-теги)
13. [Что покрыто тестами](#13-что-покрыто-тестами)
14. [Как запустить и проверить руками](#14-как-запустить-и-проверить-руками)
15. [Как я проверял](#15-как-я-проверял)
16. [Проблемы, которые встретились по пути](#16-проблемы-которые-встретились-по-пути)
17. [Словарик](#17-словарик)
18. [Что дальше](#18-что-дальше)

---

## 1. Общая картина

### Было → стало

**Было:** 188 тестов, 144 API-теста и 44 DB-теста. Они проверяют сервер и базу, а **сайт** не проверяет никто.
Сервер может считать всё правильно, а кнопка «Add to cart» при этом не нажимается или итог выводится не в той строке.

**Стало:** добавились **54 UI-сценария**. Они открывают настоящий Chrome и делают то же, что сделал бы человек:
ищут товар, кладут его в корзину, вводят промокод, оформляют заказ и отменяют его.
Всего теперь **242 теста**, все зелёные.

```
             ▲  UI (Selenium + Cucumber)    54   медленные, но проверяют то, что видит человек
            ▲▲▲ API (REST Assured)         144   быстрые, проверяют все правила сервера
           ▲▲▲▲▲ DB (JDBC)                  44   проверяют, что данные легли в базу правильно
```

### Почему UI-тестов меньше, чем API-тестов?

Это называется **пирамида тестирования**. UI-тест медленный: один сценарий занимает 2–5 секунд, а API-тест 0.05 секунды.
К тому же UI-тест падает от многих причин сразу: верстка, задержки, сеть.
Поэтому **все комбинации правил** (20 вариантов неправильного адреса, 15 вариантов промокода) проверяются на уровне API,
а в UI достаточно убедиться, что **сайт правильно показывает** то, что вернул сервер, и что форма вообще работает.

Пример: правило «доставка бесплатна от $50» проверено в API-тестах на границах ($49.99, $50.00, $50.01).
В UI проверены только два случая: «$5.99» и «FREE». Этого хватает, чтобы убедиться, что сайт выводит число из ответа сервера.

---

## 2. Что добавилось в модуль tests

```
tests/
├── pom.xml                                   + Selenium, Cucumber, PicoContainer, junit-platform-suite, allure-cucumber7-jvm
└── src/test/
    ├── java/com/shoplab/tests/
    │   ├── config/TestConfig.java            + ui.url, ui.headless, ui.timeoutSeconds
    │   └── ui/
    │       ├── RunCucumberTest.java          раннер: «запусти все .feature через Cucumber»
    │       ├── driver/DriverFactory.java     как запускается Chrome (единственное место)
    │       ├── context/UiContext.java        общее для шагов одного сценария: браузер, покупатель, номер заказа
    │       ├── hooks/Hooks.java              до всех сценариев: сброс БД; после каждого: скриншот при падении и закрытие браузера
    │       ├── support/
    │       │   ├── BrowserSession.java       «быстрый вход»: токен из API кладётся в localStorage
    │       │   └── ProductCatalog.java       название товара → id (через API)
    │       ├── pages/                        PAGE OBJECTS: по классу на страницу или компонент
    │       │   ├── BasePage.java             общие методы: ожидания, клики, ввод, toasts, ошибки полей
    │       │   ├── HeaderComponent.java      шапка (поиск, счётчик корзины, «Hello, ...», Sign out)
    │       │   ├── ConfirmModal.java         окно «Are you sure?»
    │       │   ├── CatalogPage.java  ProductPage.java  CartPage.java
    │       │   ├── LoginPage.java  RegisterPage.java  CheckoutPage.java
    │       │   └── OrderConfirmationPage.java  OrdersPage.java  NotFoundPage.java
    │       └── steps/                        STEP DEFINITIONS: перевод фраз из .feature в вызовы Page Objects
    │           ├── CommonSteps.java  CatalogSteps.java  AuthSteps.java
    │           └── CartSteps.java  CheckoutSteps.java  OrderSteps.java
    └── resources/features/                   СЦЕНАРИИ на Gherkin
        ├── catalog.feature   auth.feature   cart.feature
        └── checkout.feature  orders.feature
```

### Новые зависимости

| Библиотека | Версия | Зачем |
|---|---|---|
| `selenium-java` | 4.52.0 | Управляет браузером: открыть страницу, кликнуть, ввести текст, прочитать текст |
| `cucumber-java` | 7.34.9 | Аннотации `@Given / @When / @Then` для step definitions |
| `cucumber-junit-platform-engine` | 7.34.9 | Позволяет JUnit 5 (а значит Maven и IntelliJ) запускать сценарии Cucumber |
| `cucumber-picocontainer` | 7.34.9 | Внедрение зависимостей: один `UiContext` на сценарий для всех классов шагов |
| `junit-platform-suite` | из junit-bom | Аннотация `@Suite` для класса-раннера |
| `allure-cucumber7-jvm` | из allure-bom | Каждый шаг Given/When/Then виден в отчёте Allure |

**Почему Cucumber 7, а не 8?** Cucumber 8 уже вышел, но ему нужен **JUnit 6**, а у нас JUnit 5.14.
К тому же у Allure 2 плагин есть только для Cucumber 7 (`allure-cucumber7-jvm`).
Библиотеки должны подходить друг к другу, поэтому самая новая версия не всегда правильный выбор.
Перед обновлением всегда смотрите, от каких версий зависит библиотека: это видно в её `.pom` на Maven Central.

---

## 3. Как всё связано: от строчки сценария до клика в браузере

```
cart.feature                  CartSteps.java                      CartPage.java                 Chrome
─────────────                 ──────────────                      ─────────────                 ──────
When I apply the     ──►  @When("I apply the promo    ──►  applyPromo(code) {          ──►  вводит "QA10"
promo code "QA10"          code {string}")                    type(promo-input, code)        нажимает Apply
                           applyPromo(String code) {          click(promo-apply)             ждёт перерисовки
                             cartPage().applyPromo(code)       ...ждёт перерисовки... }
                           }

Then the discount    ──►  @Then("the discount is      ──►  discount() {                ──►  читает текст
is "-$8.00"                {string}")                         return text(summary-       "-$8.00"
                           assertThat(cartPage()               discount) }
                             .discount()).isEqualTo(..)
```

У каждого слоя своя ответственность:

| Слой | Знает | Не знает |
|---|---|---|
| **Feature-файл** | ЧТО проверяем, языком бизнеса | Как устроен сайт, локаторы, Java |
| **Step definition** | Какой метод страницы вызвать и что сравнить | Локаторы, ожидания |
| **Page Object** | Как найти элемент и как дождаться результата | Что правильно, а что нет (не делает assert) |

Если на сайте поменяется вёрстка корзины, изменится **только** `CartPage.java`. Сценарии и шаги останутся прежними.

---

## 4. Gherkin: как писать сценарии

**Gherkin** — простой язык для сценариев. Ключевые слова: `Feature`, `Background`, `Scenario`, `Scenario Outline`, `Examples`, `Given`, `When`, `Then`, `And`.

```gherkin
@ui @cart                                         ← теги для всего файла
Feature: Shopping cart                            ← что тестируем (одна фича = один файл)
  As a shopper                                    ← кто, что и зачем (необязательно, но полезно)
  I want to collect products in a cart ...

  @smoke                                          ← тег одного сценария
  Scenario: The promo code QA10 gives 10% off
    Given I am logged in as a new customer        ← Given = исходное состояние
    And my cart contains:                         ← And = продолжение предыдущего
      | product              | quantity |         ← таблица данных (DataTable)
      | Selenium in Practice | 2        |
    When I open the cart                          ← When = действие
    And I apply the promo code "QA10"
    Then I see the promo message "Code QA10 applied: 10% off your order"   ← Then = проверка
    And the discount is "-$8.00"
    And the order total is "$71.98"
```

### Scenario Outline: один сценарий, много наборов данных

Это то же самое, что `@ParameterizedTest` в JUnit. Значения из таблицы `Examples` подставляются на место `<...>`,
и каждая строка становится **отдельным тестом**:

```gherkin
  Scenario Outline: The checkout form refuses a wrong <field>
    ...
    And I change the "<field>" field to "<value>"
    And I place the order
    Then the "<field>" field shows the error "<error>"

    Examples:
      | field  | value | error                     |
      | zip    | 12    | ZIP code must be 5 digits |
      | expiry | 01/20 | Your card has expired     |
```

### Background: общие первые шаги

`Background` выполняется **перед каждым** сценарием файла. В `checkout.feature` это «войти, положить товар в корзину,
открыть checkout»: без этого ни один сценарий оформления заказа не имеет смысла.

### Правила хорошего сценария, которых я придерживался

1. **Декларативно, а не императивно.** Пишем «I fill in a valid shipping address», а не 6 строк «I type "Austin" into the city field».
   Сценарий описывает **поведение**, а не клики. Детали спрятаны в шагах и Page Objects.
2. **Ожидаемые значения прямо в сценарии.** `the order total is "$71.98"` сразу видно и проверяющему, и аналитику.
3. **Один сценарий = одно поведение.** Не «зашёл, купил, отменил, вышел» в одном сценарии.
4. **Сценарии не зависят друг от друга.** Каждый создаёт себе свежего покупателя (раздел 11) и не рассчитывает на то, что сделал предыдущий.
5. **Язык — английский.** Gherkin умеет и по-русски (`# language: ru`, «Дано / Когда / Тогда»),
   но в портфолио и в большинстве компаний сценарии пишут по-английски, а тексты сайта у нас тоже английские.

---

## 5. Step definitions: перевод фраз в Java

Step definition — это Java-метод с аннотацией, текст которой совпадает со строкой сценария:

```java
@When("I change the quantity of {string} to {int}")
public void changeQuantity(String productTitle, int quantity) {
    cartPage().changeQuantity(productTitle, quantity);
}
```

Cucumber берёт строку `When I change the quantity of "Selenium in Practice" to 3`, находит подходящую аннотацию,
вырезает `"Selenium in Practice"` в `String` и `3` в `int` и вызывает метод.

### Cucumber expressions: что можно писать в аннотации

| Запись | Что совпадает | Пример |
|---|---|---|
| `{string}` | текст в `"двойных"` **или** `'одинарных'` кавычках | `"QA10"`, `'Promo code "NOPE" is not valid'` |
| `{int}` | целое число | `3` |
| `(s)` | **необязательный** текст | `order(s)` совпадает и с «1 order», и с «2 orders» |
| `\"Show\"` | обычные кавычки как часть текста | `I click "Show" next to the password` |

Одинарные кавычки нужны, когда в самом тексте есть двойные: `'No results found for "qwerty"'`.

### DataTable: таблица в шаге

```java
@Given("my cart contains:")
public void myCartContains(List<Map<String, String>> rows) {   // одна Map на строку: заголовок → значение
    ...
}

@Then("I see the products:")
public void seeTheProducts(List<String> titles) { ... }        // таблица из одной колонки → List<String>
```

### Правила для шагов

- **Шаг короткий:** 1–3 строки, то есть вызов Page Object и `assertThat`.
- **В шаге нет `driver.findElement`.** Искать элементы — работа страницы.
- **Проверки (`assertThat`) только в шагах `Then`.** Page Object лишь читает значения.
- **Один шаг может иметь несколько формулировок:** на метод можно повесить две аннотации:
  ```java
  @Given("I am on the sign in page")
  @When("I go to the sign in page")
  public void openSignInPage() { loginPage().open(); }
  ```
- **Одна фраза — один шаг во всём проекте.** Cucumber ищет шаги во всех классах сразу (glue). Если одинаковый текст есть в двух классах, будет ошибка «duplicate step».

---

## 6. Page Object Model

**Page Object** — класс, который описывает одну страницу сайта: какие на ней есть элементы и что на ней можно сделать.
Тест говорит `cartPage.applyPromo("QA10")`, а не «найди поле с таким-то селектором, напечатай, найди кнопку, нажми».

### BasePage: общее для всех страниц

```java
public static By testId(String id) {
    return By.cssSelector("[data-testid='" + id + "']");
}
protected WebElement visible(By locator)   // ждёт, пока элемент станет видимым
protected void click(By locator)           // ждёт, пока можно кликнуть, потом кликает
protected void type(By locator, String t)  // очищает поле и печатает
protected String text(By locator)          // ждёт видимости и читает текст
public boolean waitForToast(String msg)    // ждёт всплывающее сообщение с этим текстом
public String fieldError(String field)     // красный текст под полем: "<field>-error"
```

### Правила, которых придерживаются все страницы

1. **Локаторы только через `data-testid`.** Они не меняются, когда дизайнер меняет текст кнопки или CSS-класс.
   Поэтому в проекте действует правило: никогда не удалять и не переименовывать `data-testid`.
2. **Никаких assert в Page Object.** Страница отвечает «что показано», а решает «правильно ли это» шаг.
3. **Метод-действие возвращается, только когда сайт закончил реагировать** (подробно в разделе 7).
4. **Элементы ищутся заново при каждом обращении** (`By`, а не сохранённый `WebElement`).
   Наш сайт перерисовывает страницу целиком (`app.innerHTML = ...`). Сохранённый элемент после этого
   становится «протухшим», и обращение к нему даёт `StaleElementReferenceException`.
   По той же причине не используется `PageFactory` с `@FindBy`.

### Компоненты

Шапка есть на **каждой** странице, поэтому она не страница, а **компонент**: `HeaderComponent`.
То же с окном подтверждения: `ConfirmModal`. Страницы не копируют код шапки, а шаги создают компонент там, где он нужен.

### Найти строку по тексту

В корзине несколько строк `cart-item`. Нужная ищется по названию товара:

```java
private WebElement item(String productTitle) {
    return wait.until(d -> d.findElements(CART_ITEM).stream()
            .filter(row -> row.findElement(testId("cart-item-title")).getText().trim().equals(productTitle))
            .findFirst()
            .orElse(null));   // null = «ещё нет», ожидание попробует снова
}
```

`wait.until(лямбда)` повторяет лямбду, пока она не вернёт что-то, кроме `null` или `false`, или пока не выйдет время.
Это собственное ожидание, написанное за одну строку.

---

## 7. Ожидания: самое важное в UI-тестах

Сайт отвечает **не сразу**: каталог грузится минимум 600 мс, вход 400 мс, оплата 1500 мс, а пользователь `slow@shoplab.test` входит 3 секунды.
Эти задержки сделаны специально, чтобы учиться ждать правильно.

### Три способа ждать

| Способ | Как работает | Почему (не) используем |
|---|---|---|
| `Thread.sleep(2000)` | Всегда ждёт 2 секунды | ❌ Если сайт ответил за 0.5 с, теряем 1.5 с. Если за 2.5 с, тест падает. Плохо в обоих случаях |
| Implicit wait | Каждый `findElement` ждёт до N секунд, пока элемент появится | ❌ Умеет ждать только **появления** элемента, а не «текст изменился» или «кнопка стала активной». При смешивании с явными ожиданиями время непредсказуемо |
| **Explicit wait** (`WebDriverWait`) | Ждёт **конкретного условия**, проверяя его каждые 0.5 с, максимум N секунд | ✅ Возвращается сразу, как только условие выполнено |

У нас только явные ожидания. Таймаут 10 секунд (`-Dui.timeoutSeconds`), но это **максимум**:
обычно ожидание заканчивается за доли секунды. Вход медленного пользователя (3 с) проходит без каких-либо особых настроек.

### Главное правило: «действие ждёт своего окончания»

Каждый метод-действие Page Object возвращается, только когда сайт **закончил** реагировать.
Тогда следующий шаг может сразу читать страницу и не думать о времени:

```java
// ProductPage: добавление в корзину заканчивается всплывающим "Added to cart: <название>"
public void addToCart(int quantity) {
    ...
    click(testId("add-to-cart"));
    wait.until(d -> toasts().contains("Added to cart: " + name));
}
```

### Ловушка: старая страница ещё на экране

Нажали «страница 2». Сайт идёт в API, а **старые** 8 товаров пока на экране.
Если тест сразу прочитает список, он получит товары первой страницы и упадёт (или, что хуже, пройдёт, хотя не должен).
Это **race condition** (гонка).

Решение — дождаться, пока старое содержимое **исчезнет**. Сайт заменяет всё внутри `<main id="app">`,
поэтому старый элемент становится «stale» (удалён со страницы):

```java
protected void redrawsPage(Runnable action) {
    List<WebElement> before = driver.findElements(By.cssSelector("#app > *"));   // запомнили старое
    action.run();                                                                  // нажали
    if (!before.isEmpty()) {
        wait.until(ExpectedConditions.stalenessOf(before.getFirst()));             // ждём, пока старое исчезнет
    }
}

public CatalogPage goToPage(int number) {
    redrawsPage(() -> click(testId("page-" + number)));
    return waitUntilLoaded();     // и пока появится новое: results-count рисуется только после ответа API
}
```

### Когда результат действия бывает разным

`LoginPage.signIn(...)` **не ждёт** ничего после клика: при успехе откроется другая страница,
при ошибке появится красная рамка, у медленного пользователя будет 3 секунды спиннера.
Ждёт следующий шаг, и ждёт он того, что **ожидает именно он**: `header().greeting()` ждёт приветствие,
`loginPage().errorMessage()` ждёт текст ошибки.

---

## 8. Браузер: DriverFactory, headless, Selenium Manager

```java
public static WebDriver createChrome() {
    ChromeOptions options = new ChromeOptions();
    if (TestConfig.uiHeadless()) {
        options.addArguments("--headless=new");     // без окна
    }
    options.addArguments("--window-size=1366,900"); // одинаковый размер везде
    options.setExperimentalOption("prefs", Map.of(  // выключить «Сохранить пароль?» и «Ваш пароль утёк»
            "credentials_enable_service", false, ...));
    return new ChromeDriver(options);
}
```

- **Где chromedriver.exe?** Его нет в проекте. Начиная с Selenium 4.6, встроенный **Selenium Manager** сам скачивает
  драйвер под установленную версию Chrome и кэширует его в `~/.cache/selenium`. Когда Chrome обновится, новый драйвер скачается сам.
- **Зачем фиксированный размер окна?** Headless Chrome по умолчанию открывается 800×600. На узком экране сайт
  перестраивает вёрстку (`@media (max-width:900px)` в `index.html`), и тест мог бы проходить только на одной машине.
- **Зачем выключать менеджер паролей?** Наш тестовый пароль `Password123` публичный, и Chrome может показать
  окно «пароль найден в утечке». Окно перекроет страницу, и клик не попадёт в кнопку.
- **Почему фабрика?** Это единственное место, которое знает, как запускается браузер. Когда понадобится Firefox
  или удалённый Selenium Grid, поменяется только этот класс.

### Headless или с окном

| Режим | Как включить | Когда |
|---|---|---|
| С окном (по умолчанию) | ничего не делать | На своём компьютере: видно, что делает тест |
| Headless | `-Dui.headless=true` или `$env:SHOPLAB_UI_HEADLESS="true"` | Быстрее, не мешает работать; на CI-сервере окна нет вообще (шаг 7) |

---

## 9. UiContext и PicoContainer: общий браузер для всех шагов

Шаги одного сценария лежат в **разных** классах: «войти» в `AuthSteps`, «положить в корзину» в `CartSteps`,
«оформить» в `CheckoutSteps`. Но работать они должны с **одним и тем же** браузером.

Для этого есть `UiContext`: объект, в котором хранится всё общее для **одного сценария**:

```java
public class UiContext {
    private WebDriver driver;       // браузер (создаётся при первом обращении)
    private LoggedInUser customer;  // покупатель, созданный через API
    private String orderNumber;     // номер заказа, чтобы найти его на странице «Orders»
}
```

Каждый класс шагов просит его в конструкторе:

```java
public class CartSteps {
    private final UiContext ui;
    public CartSteps(UiContext ui) { this.ui = ui; }   // PicoContainer передаст сюда объект сам
}
```

**PicoContainer** — маленькая библиотека **внедрения зависимостей** (dependency injection). Перед каждым сценарием
она создаёт **новый** `UiContext` и передаёт **один и тот же** объект во все классы шагов и в `Hooks`.
Поэтому шаги одного сценария видят общий браузер, а два разных сценария не делят ничего.

Браузер создаётся **лениво** (lazy): при первом вызове `ui.driver()`, а не перед каждым сценарием.

---

## 10. Hooks: подготовка и скриншот при падении

**Hooks** — код, который Cucumber выполняет вокруг сценариев сам. В `.feature` их не видно.

```java
@BeforeAll                               // один раз перед ВСЕМИ сценариями
public static void prepareEnvironment() {
    support.waitUntilApiIsHealthy();     // как в BaseTest: дождаться API
    support.resetDatabase();             // и вернуть исходные данные
}

@After                                   // после КАЖДОГО сценария, даже упавшего
public void closeBrowser(Scenario scenario) {
    try {
        if (scenario.isFailed()) {
            scenario.attach(скриншот, "image/png", "Screenshot");
            scenario.attach(адрес страницы, "text/plain", "Page URL");
            scenario.attach(HTML страницы, "text/html", "Page HTML");
        }
    } finally {
        ui.quitDriver();                 // закрыть Chrome в любом случае
    }
}
```

- **Скриншот при падении** часто объясняет проблему сразу: «а, кнопку закрыло всплывающее окно».
  `scenario.attach(...)` кладёт файл и в HTML-отчёт Cucumber, и в Allure (там он виден в разделе **Tear down** сценария).
- **`finally`** гарантирует, что браузер закроется, даже если сам скриншот не получился.
  Иначе после каждого упавшего теста в памяти оставался бы лишний `chrome.exe`.
- **Почему один новый браузер на сценарий?** Новый Chrome — это пустой localStorage: нет гостевой корзины, нет токена, нет wishlist
  от предыдущего сценария. Это медленнее (+1–2 с на сценарий), но сценарии гарантированно независимы.

---

## 11. Быстрая подготовка через API

### «Быстрый вход» без формы входа

Форму входа проверяют сценарии в `auth.feature`. Сценариям корзины и заказа она не нужна: им нужен просто вошедший покупатель.
Сайт хранит токен в `localStorage` под ключом `shoplab_token`. Поэтому:

```java
public static void signInWithToken(WebDriver driver, String token) {
    driver.get(TestConfig.uiUrl() + "/#/");                     // localStorage принадлежит сайту: сначала открыть сайт
    ((JavascriptExecutor) driver).executeScript(
        "localStorage.setItem('shoplab_token', JSON.stringify(arguments[0]));", token);
    driver.navigate().refresh();                                // сайт читает токен и спрашивает GET /api/users/me
    new HeaderComponent(driver).greeting();                     // ждём "Hello, Test"
}
```

Шаг `Given I am logged in as a new customer` регистрирует **нового** покупателя через API (`TestUsers.registerNew()` из шага 3)
и входит этим способом. Новый покупатель — это пустая корзина и пустая история заказов, поэтому сценарии не мешают друг другу.

### Корзина и заказ через API

```gherkin
And my cart contains:                                 ← CartClient.addItem(...) через REST
  | Selenium in Practice | 2 |
And I have placed an order for 1 "Coffee Maker Brew 12"   ← CartClient + OrdersClient
```

Сценарий про **страницу истории заказов** не должен проходить через каталог, корзину и checkout.
Если сломается checkout, упадут сценарии checkout, а не все подряд. Это и называется **подготовка через API, проверка через UI**.
Так работают в реальных проектах.

### Названия товаров вместо id

Сценарии пишут «Selenium in Practice», а не «product 7». Когда шагу нужен id (открыть `#/product/7` или положить товар
в корзину через API), он спрашивает `ProductCatalog.idOf("Selenium in Practice")`. Класс один раз берёт список товаров из API и хранит его в памяти.

---

## 12. Раннер и теги

Maven и IntelliJ умеют запускать только JUnit. Класс `RunCucumberTest` — это JUnit-«сьют», который передаёт работу Cucumber:

```java
@Suite(failIfNoTests = false)                     // пустой сьют (например, при -Dgroups=api) — не ошибка
@IncludeEngines("cucumber")                       // запускает движок Cucumber
@SelectPackages("features")                       // сценарии лежат в src/test/resources/features
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.shoplab.tests.ui")     // где искать шаги и hooks
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME,
        value = "pretty, html:target/cucumber-report.html, io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm")
public class RunCucumberTest { }
```

Плагины:
- `pretty` — красивый вывод сценариев в консоль;
- `html:target/cucumber-report.html` — простой HTML-отчёт Cucumber (одна страница, открывается в браузере);
- `AllureCucumber7Jvm` — результаты для Allure (шаг 6).

### Теги

Теги сценариев (`@ui`, `@smoke`, `@cart`...) превращаются в теги JUnit, поэтому **одна и та же команда** фильтрует API-, DB- и UI-тесты:

| Команда | Что запустится | Сколько |
|---|---|---|
| `mvn -pl tests test` | всё | 242 |
| `-Dgroups=ui` | только UI | 54 |
| `-DexcludedGroups=ui` | всё, кроме UI (быстро, без браузера) | 188 |
| `-Dgroups=smoke` | smoke со всех уровней | 29 (22 API/DB + 7 UI) |
| `"-Dgroups=ui & smoke"` | только UI smoke | 7 |
| `-Dgroups=cart` | только сценарии корзины | 10 |
| `-Dgroups=slow` | медленные тесты всех уровней (вход пользователя slow за 3 с) | 2 (1 API + 1 UI) |

Теги по фичам: `catalog`, `auth`, `cart`, `checkout`, `orders`.

---

## 13. Что покрыто тестами

| Файл | Сценариев | Что проверяется |
|---|---|---|
| `catalog.feature` | 14 | первая страница (8 товаров, «1-8 of 25»), поиск (3 слова + «No results»), фильтр по отделу, сортировка по цене, фильтр цены и 3 ошибки цены, пагинация, товар «нет в наличии», страница 404 |
| `auth.feature` | 17 | вход, 3 вида неверных данных (неверный пароль, нет такого email, заблокирован), 3 проверки полей, медленный вход, кнопка «Show», защищённая страница → вход → возврат, выход, регистрация, повторный email, 4 проверки формы регистрации |
| `cart.feature` | 10 | гость кладёт товар из поиска и со страницы товара, гостевая корзина переходит в аккаунт при входе, изменение количества, удаление с подтверждением и отмена, доставка $5.99 / FREE с подсказкой, промокод QA10, неверный промокод |
| `checkout.feature` | 11 | оплата картой → подтверждение и пустая корзина, наложенный платёж (поля карты скрыты), экспресс-доставка $9.99, отклонённая карта, 7 ошибок формы |
| `orders.feature` | 2 | пустая история, заказ в истории → отмена → статус «Cancelled» |
| **Итого** | **54** | 7 из них `@smoke` |

---

## 14. Как запустить и проверить руками

### 1) Запустить магазин

```powershell
docker compose up -d --build
docker compose ps          # postgres и backend "healthy", frontend "Up"
```

Нужен установленный **Google Chrome**. Драйвер скачается сам при первом запуске (нужен интернет).

### 2) Запустить тесты

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\openjdk-21.0.1"

mvn -pl tests test "-Dgroups=ui & smoke"            # 7 сценариев, ~30 с: смотрите, как Chrome кликает сам
mvn -pl tests test -Dgroups=ui                      # все 54 UI-сценария, ~2.5 мин
mvn -pl tests test -Dgroups=ui -Dui.headless=true   # то же без окон
mvn -pl tests test                                  # все 242 теста
```

> В PowerShell выражение с `&` нужно брать в кавычки целиком: `"-Dgroups=ui & smoke"`.

**Ожидаемый результат:** `Tests run: 54, Failures: 0, Errors: 0` и `BUILD SUCCESS`.

### 3) Посмотреть отчёт Cucumber

Откройте в браузере файл `tests/target/cucumber-report.html`. Там видны все сценарии, шаги, время и, если что-то упало, скриншот.

### 4) Запустить из IntelliJ

1. Поставьте плагины **Gherkin** и **Cucumber for Java** (Settings → Plugins). Тогда `.feature`-файлы подсвечиваются,
   а **Ctrl+клик по шагу** переходит в его Java-метод.
2. Самый простой запуск: правый клик по `RunCucumberTest.java` → **Run**.
3. Один сценарий: зелёная стрелка рядом со `Scenario:` в `.feature`-файле. Если IntelliJ спросит, укажите glue `com.shoplab.tests.ui`.
4. В конфигурации запуска (Run → Edit Configurations) в поле **JRE** выберите JDK 21.

### 5) Пройти сценарий руками

Возьмите `checkout.feature` → «A customer pays by card» и повторите его в браузере на http://localhost:3000:
зарегистрируйтесь, положите 2 × «Selenium in Practice», откройте корзину → «Proceed to checkout», заполните адрес
(ZIP `73301`, телефон `+1 555 123 4567`), карта `4242 4242 4242 4242`, срок в будущем, CVV `123`.
Итог должен быть **$79.98**, счётчик корзины после заказа **0**. Это ровно то, что проверяет тест.

### 6) Упражнение: сломать сайт и поймать поломку

1. В `frontend/index.html` найдите `'Passwords do not match'` и поменяйте на `'Passwords are different'`.
2. Пересоберите сайт: `docker compose up -d --build frontend` (файлы сайта вшиты в Docker-образ).
3. `mvn -pl tests test -Dgroups=auth -Dui.headless=true` → должен упасть **ровно 1** сценарий:
   `expected: "Passwords do not match" but was: "Passwords are different"`.
4. Откройте `tests/target/cucumber-report.html`: у упавшего сценария есть скриншот формы с новым текстом.
5. **Верните текст обратно** и снова выполните `docker compose up -d --build frontend`.

Ещё один вариант: в `cart.feature` поменяйте ожидаемый итог `"$71.98"` на `"$71.99"`, запустите `-Dgroups=cart`,
посмотрите на падение и скриншот, затем верните значение.

### 7) Проверить, что медленный вход ждётся правильно

Запустите `mvn -pl tests test -Dgroups=slow` и посмотрите время сценария «A slow sign in still succeeds».
Будет около 3–5 секунд, а не 10: явное ожидание заканчивается сразу, как только появилось приветствие.

---

## 15. Как я проверял

| Проверка | Результат |
|---|---|
| Полный прогон `mvn -pl tests test` | 242 теста, 0 падений |
| UI-тесты два раза подряд | 54 / 54 оба раза (нестабильных тестов нет) |
| `-Dgroups=smoke` / `db` / `-DexcludedGroups=ui` | 29 / 44 / 188 |
| `"-Dgroups=ui & smoke"` с видимым окном Chrome | 7 / 7 |
| Специально неверный итог в сценарии промокода | упал именно он: `expected "$71.99" but was "$71.98"`; в Allure есть PNG-скриншот, URL и HTML страницы |
| Дубли в Allure (могли появиться из-за двух слушателей, JUnit и Cucumber) | 242 теста = 242 результата, дублей нет |

---

## 16. Проблемы, которые встретились по пути

**1. Cucumber 8 не подошёл.**
Самая новая версия Cucumber требует JUnit 6, а плагин Allure есть только для Cucumber 7.
**Решение:** Cucumber 7.34.9, последняя версия ветки 7, собранная под JUnit 5.14.
**Урок:** перед выбором версии смотрите зависимости библиотеки, а не только номер.

**2. Шаги «не найдены» (6 сценариев).**
В сценариях я написал `Then the catalog shows 1 result(s)`, буквально со скобками. А в Cucumber expression `(s)`
означает «необязательная буква s», то есть выражение совпадает с текстом «1 result» или «5 results», но не с текстом «result(s)».
Cucumber сообщил `UndefinedStepException: The step 'the catalog shows 1 result(s)' is undefined`.
**Решение:** в сценариях обычный английский: «shows 5 results».

**3. Раннер падал при `-DexcludedGroups=ui`.**
Когда фильтр по тегам убирает все сценарии, JUnit-сьют по умолчанию считает это ошибкой: `NoTestsDiscoveredException`.
**Решение:** `@Suite(failIfNoTests = false)`.

**4. Текст с кавычками внутри.**
`"Promo code "NOPE" is not valid"` ломает `{string}`: кавычка внутри закрывает строку.
**Решение:** `{string}` принимает и одинарные кавычки: `'Promo code "NOPE" is not valid'`.

**5. PowerShell испортил feature-файл.**
При проверке я менял файл командой `Set-Content -Encoding utf8`. Windows PowerShell 5.1 в этом режиме дописывает в начало
невидимые байты **BOM** (`EF BB BF`). **Решение:** убрал BOM и дальше правил файлы только редактором и `sed`.
**Урок:** в PowerShell 5.1 «utf8» означает «UTF-8 с BOM». Если файл после правки ведёт себя странно, проверьте первые байты.

**6. Предупреждение Cucumber про `@SelectClasspathResource`.**
Cucumber советует выбирать папку с фичами через `@SelectPackages("features")`. Поменял, предупреждение исчезло.

**7. Ловушки, которые я обошёл заранее:**
- **Гонка при перерисовке** (раздел 7): без `redrawsPage` тест пагинации иногда читал бы товары старой страницы.
- **Окно «пароль утёк»** в Chrome: выключено в `DriverFactory`, иначе оно могло бы перекрыть кнопку.
- **Срок действия карты:** берётся `Cards.futureExpiry()` (всегда +2 года), а «истёкшая карта» — это `01/20`, прошлое навсегда.

---

## 17. Словарик

| Термин | Простыми словами |
|---|---|
| **Selenium WebDriver** | Библиотека, которая управляет настоящим браузером из кода |
| **WebDriver / ChromeDriver** | Объект «браузер» в Java; ChromeDriver — его версия для Chrome |
| **chromedriver** | Программа-посредник между Selenium и Chrome |
| **Selenium Manager** | Часть Selenium, которая сама скачивает подходящий chromedriver |
| **Headless** | Режим браузера без окна |
| **Локатор (By)** | Способ найти элемент: CSS-селектор, XPath, id... |
| **data-testid** | Специальный атрибут для тестов; не меняется вместе с дизайном |
| **Page Object** | Класс-описание страницы: элементы и действия на ней |
| **Компонент** | Page Object для части страницы, которая повторяется (шапка, модальное окно) |
| **Explicit wait** | Ожидание конкретного условия с таймаутом (`WebDriverWait`) |
| **Implicit wait** | Глобальное ожидание появления элемента для каждого `findElement` (не используем) |
| **Stale element** | «Протухший» элемент: его уже удалили со страницы |
| **Race condition (гонка)** | Результат зависит от того, что случится раньше: перерисовка или чтение |
| **Flaky test** | Нестабильный тест: то проходит, то падает без изменений в коде |
| **Cucumber** | Инструмент, который запускает сценарии на Gherkin |
| **Gherkin** | Язык сценариев: `Feature`, `Scenario`, `Given / When / Then` |
| **BDD** | Behaviour-Driven Development: описываем поведение системы примерами, понятными всей команде |
| **Feature-файл** | Файл `.feature` со сценариями одной функции |
| **Step definition** | Java-метод, который выполняет одну строку сценария |
| **Glue** | Пакет(ы), где Cucumber ищет step definitions и hooks |
| **Cucumber expression** | Шаблон в аннотации шага: `{string}`, `{int}`, `(s)` |
| **Scenario Outline / Examples** | Шаблон сценария и таблица данных для него, аналог параметризованного теста |
| **Background** | Шаги, которые выполняются перед каждым сценарием файла |
| **DataTable** | Таблица прямо внутри шага |
| **Hook** | Код до или после сценариев (`@BeforeAll`, `@After`) |
| **Dependency injection (DI)** | Объект не создаёт свои зависимости сам, их передают ему в конструктор |
| **PicoContainer** | Маленькая DI-библиотека, которую использует Cucumber |
| **JUnit Platform Suite** | Класс с `@Suite`, который собирает и запускает тесты другого движка |
| **Пирамида тестирования** | Много быстрых низкоуровневых тестов, мало медленных UI-тестов |
| **BOM (Byte Order Mark)** | Невидимые байты в начале текстового файла (не путать с Maven BOM — списком версий) |

---

## 18. Что дальше

**Шаг 6. Allure-отчёты.** Результаты уже собираются в `tests/target/allure-results`: HTTP-запросы, SQL-запросы,
шаги Cucumber и скриншоты. На шаге 6 настроим сам отчёт: шаги, вложения, категории ошибок, информация об окружении,
и научимся открывать его локально командой `allure serve`.

Полный план из 8 шагов — в [CLAUDE.md](../CLAUDE.md).
