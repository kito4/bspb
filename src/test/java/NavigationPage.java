import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class NavigationPage {

    private WebDriver driver;
    private WebDriverWait wait;

    public static final List<String> MENU_ITEMS = List.of(
            "Частным клиентам",
            "Бизнесу",
            "ВЭД",
            "Финансовые рынки",
//            "Private Banking", //так как это вообще другая страница банка то нужен отдельный тест ?
            "Инвесторам"
    );

    public NavigationPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    private By menuItem(String text) {
        return By.linkText(text);
    }

    public void open() {
        driver.get("https://www.bspb.ru/");
    }

    public void clickMenuItem(String text) {
        WebElement element = wait.until(
                ExpectedConditions.elementToBeClickable(menuItem(text))
        );
        element.click();
    }

    public String getMenuItemColor(String text) {
        WebElement element = wait.until(
                ExpectedConditions.visibilityOfElementLocated(menuItem(text))
        );
        return element.getCssValue("color");
    }
}
