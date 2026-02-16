import org.junit.jupiter.api.Assertions;
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
    public static final String Contacts = "Контакты";
    public static final List<String> Contacts_for_different_group = List.of(
            "//button[contains(.,'Бизнесу')]",
            "//button[text()='ВЭД']");
    public static final List<String> MENU_ITEMS = List.of(
            "Частным клиентам",
            "Бизнесу",
            "ВЭД",
            "Финансовые рынки",
//            "Private Banking", //так как это вообще другая страница банка то нужен отдельный тест ?
            "Инвесторам",
            "Контакты",
            "Офисы"
    );

    public NavigationPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    private By menuItem(String text) {
        return By.linkText(text);
    }

    public void openMainPage() {
        driver.get("https://www.bspb.ru/");
    }

    public boolean CheckURL(String url) {
        String currentUrl = driver.getCurrentUrl();
        if (currentUrl.equals(url)) {
            return true;
        }
        return false;
    }
    public void clickButton(String xpath){
        WebElement tab = wait.until(
                ExpectedConditions.elementToBeClickable(By.xpath(xpath))
        );
        tab.click();
    }
    public void clickMenuItem(String text) {
        WebElement element = wait.until(
                ExpectedConditions.elementToBeClickable(menuItem(text))
        );
        element.click();
    }
    public String getItemColor_by_xpath(String text) {
        WebElement tab = wait.until(
                ExpectedConditions.elementToBeClickable(By.xpath(text))
        );
        return tab.getCssValue("color");
    }
    public String getMenuItemColor(String text) {
        WebElement element = wait.until(
                ExpectedConditions.visibilityOfElementLocated(menuItem(text))
        );
        return element.getCssValue("color");
    }
    public void ClickByXpath(String text) {
        WebElement element = wait.until(
                ExpectedConditions.elementToBeClickable(By.xpath(text))
        );
        element.click();
    }
    public double getWidthByLink(String text){
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(By.linkText(text)));
        return Double.parseDouble(element.getCssValue("width").replace("px", ""));
    }
}
