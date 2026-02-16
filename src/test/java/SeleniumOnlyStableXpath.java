import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.Assertions;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

/**
 * Генерирует "стабильные" XPath-кандидаты без выполнения JS.
 * Алгоритм: перебирает все элементы, фильтрует невидимые,
 * пытается сформировать XPath по приоритету: id, data-*, name, aria-label, alt (img), короткий текст.
 * Для каждого кандидата проверяет, что xpath уникален (ровно 1 элемент).
 */
public class SeleniumOnlyStableXpath {

    private static final String TARGET_URL = "https://www.bspb.ru/"; // <-- заменить на BSPB

    private static ChromeOptions baseOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new"); // убрать, если нужен видимый браузер
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1280,800");
        return options;
    }

    // Вспомогательная функция для корректного встраивания строк в XPath-литерал
    private static String xpathLiteral(String s) {
        if (s == null) return "''";
        if (!s.contains("'")) return "'" + s + "'";
        // если есть одинарные кавычки, используем concat
        String[] parts = s.split("'");
        StringBuilder sb = new StringBuilder("concat(");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(", \"'\", ");
            sb.append("'").append(parts[i]).append("'");
        }
        sb.append(")");
        return sb.toString();
    }

    // Сбор кандидатов: возвращает Stream<Arguments> для параметризованного теста:
    // Arguments.of(xpath, reason, tagName, sampleText)
    static Stream<Arguments> stableXpaths() {
        WebDriver driver = new ChromeDriver(baseOptions());
        try {
            driver.get(TARGET_URL);

            // Ждём загрузки <body>
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

            // собираем все элементы (можно ограничить селектор для ускорения)
            List<WebElement> all = driver.findElements(By.cssSelector("*"));

            Set<String> seenXPaths = new LinkedHashSet<>();
            List<Arguments> found = new ArrayList<>();

            String[] dataAttrs = {"data-test", "data-testid", "data-qa", "data-test-id"};

            for (WebElement el : all) {
                try {
                    // исключаем невидимые элементы
                    if (!el.isDisplayed()) continue;
                } catch (StaleElementReferenceException e) {
                    continue;
                } catch (WebDriverException e) {
                    continue;
                }

                // priority 1: id
                String id = el.getAttribute("id");
                if (id != null && !id.isEmpty()) {
                    String xp = "//*[@id=" + xpathLiteral(id) + "]";
                    if (isUnique(driver, xp) && seenXPaths.add(xp)) {
                        found.add(Arguments.of(xp, "id", el.getTagName(), shorten(el.getText())));
                        continue; // id лучший вариант для этого элемента
                    }
                }

                // priority 2: data-*
                boolean dataFound = false;
                for (String a : dataAttrs) {
                    String v = el.getAttribute(a);
                    if (v != null && !v.isEmpty()) {
                        String xp = "//*[@"+a+"=" + xpathLiteral(v) + "]";
                        if (isUnique(driver, xp) && seenXPaths.add(xp)) {
                            found.add(Arguments.of(xp, a, el.getTagName(), shorten(el.getText())));
                            dataFound = true;
                            break;
                        }
                    }
                }
                if (dataFound) continue;

                // priority 3: name
                String name = el.getAttribute("name");
                if (name != null && !name.isEmpty()) {
                    String xp = "//*[@name=" + xpathLiteral(name) + "]";
                    if (isUnique(driver, xp) && seenXPaths.add(xp)) {
                        found.add(Arguments.of(xp, "name", el.getTagName(), shorten(el.getText())));
                        continue;
                    }
                }

                // priority 4: aria-label
                String aria = el.getAttribute("aria-label");
                if (aria != null && !aria.isEmpty()) {
                    String xp = "//*[@aria-label=" + xpathLiteral(aria) + "]";
                    if (isUnique(driver, xp) && seenXPaths.add(xp)) {
                        found.add(Arguments.of(xp, "aria-label", el.getTagName(), shorten(el.getText())));
                        continue;
                    }
                }

                // priority 5: img alt
                if ("img".equalsIgnoreCase(el.getTagName())) {
                    String alt = el.getAttribute("alt");
                    if (alt != null && !alt.isEmpty()) {
                        String xp = "//img[@alt=" + xpathLiteral(alt) + "]";
                        if (isUnique(driver, xp) && seenXPaths.add(xp)) {
                            found.add(Arguments.of(xp, "alt", "img", shorten(alt)));
                            continue;
                        }
                    }
                }

                // priority 6: короткий уникальный текст
                String txt;
                try {
                    txt = el.getText();
                } catch (StaleElementReferenceException e) {
                    txt = null;
                }
                if (txt != null) {
                    txt = txt.trim().replaceAll("\\s+", " ");
                    if (txt.length() > 1 && txt.length() < 80 && txt.matches(".*[A-Za-zА-Яа-я0-9].*")) {
                        String xp = "//*[normalize-space(text())=" + xpathLiteral(txt) + "]";
                        if (isUnique(driver, xp) && seenXPaths.add(xp)) {
                            found.add(Arguments.of(xp, "text", el.getTagName(), shorten(txt)));
                        }
                    }
                }
            }
            exportXpaths(found);
            return found.stream();

        } finally {
            driver.quit();
        }
    }
    private static void exportXpaths(List<Arguments> found) {
        try {
            List<String> lines = new ArrayList<>();
            lines.add("xpath;reason;tag;sampleText");

            for (Arguments a : found) {
                Object[] v = a.get();
                String xpath = String.valueOf(v[0]).replace(";", ",");
                String reason = String.valueOf(v[1]).replace(";", ",");
                String tag = String.valueOf(v[2]).replace(";", ",");
                String text = String.valueOf(v[3]).replace(";", ",");

                lines.add(xpath + ";" + reason + ";" + tag + ";" + text);
            }

            Files.write(
                    Path.of("generated_xpaths.csv"),
                    lines,
                    StandardCharsets.UTF_8
            );

            System.out.println("Saved " + (lines.size()-1) + " xpaths to generated_xpaths.csv");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // Проверка уникальности XPath на странице
    private static boolean isUnique(WebDriver driver, String xpath) {
        try {
            return driver.findElements(By.xpath(xpath)).size() == 1;
        } catch (InvalidSelectorException e) {
            return false;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private static String shorten(String s) {
        if (s == null) return "";
        s = s.trim().replaceAll("\\s+", " ");
        return s.length() <= 120 ? s : s.substring(0, 117) + "...";
    }


    @ParameterizedTest(name = "[{index}] xpath={0} reason={1} tag={2}")
    @MethodSource("stableXpaths")
    void checkElementByGeneratedXpath(String xpath, String reason, String tagName, String sampleText) {
        WebDriver driver = new ChromeDriver(baseOptions());
        try {
            driver.get(TARGET_URL);
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

            // Установим размер, чтобы медиазапросы работали предсказуемо
            driver.manage().window().setSize(new Dimension(1280, 800));

            List<WebElement> els = driver.findElements(By.xpath(xpath));
            Assertions.assertFalse(els.isEmpty(), "Element not found for xpath: " + xpath + " reason: " + reason);
            Assertions.assertTrue(els.get(0).isDisplayed(), "Element present but not displayed - xpath: " + xpath + " sample: " + sampleText);
        } finally {
            driver.quit();
        }
    }
}
