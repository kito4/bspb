import org.junit.jupiter.api.Test;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Navigation extends BaseTest{
    private boolean acceptNextAlert = true;
    private StringBuffer verificationErrors = new StringBuffer();

    @Test
    void testMainMenuNavigation() {
        NavigationPage navigationPage = new NavigationPage(driver);
        SoftAssertions softly = new SoftAssertions();
        for (String item : NavigationPage.MENU_ITEMS) {
            navigationPage.openMainPage();
            navigationPage.clickMenuItem(item);
            //System.out.println(item); debug
            String actualColor = navigationPage.getMenuItemColor(item);
            softly.assertThat(actualColor).isEqualTo("rgba(21, 33, 38, 1)");
            softly.assertAll();
        }
    }

    @Test
    void testMainMenuContacts(){
        SoftAssertions softly = new SoftAssertions();
        NavigationPage navigationPage = new NavigationPage(driver);
        navigationPage.openMainPage();
        navigationPage.clickMenuItem(NavigationPage.Contacts);
        softly.assertThat(navigationPage.CheckURL("https://www.bspb.ru/feedback"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"//button[contains(.,'Бизнесу')]", "//button[text()='ВЭД']"})
    void testMainMenuContacts_for_different_group(String ContactsGroups){
        SoftAssertions softly = new SoftAssertions();
        NavigationPage navigationPage = new NavigationPage(driver);
        navigationPage.openMainPage();
        navigationPage.clickMenuItem(NavigationPage.Contacts);
        navigationPage.clickButton(ContactsGroups);
        String actualColor = navigationPage.getItemColor_by_xpath(ContactsGroups); //need to use camel case style
        softly.assertThat(actualColor).isEqualTo("rgba(21, 33, 38, 1)"); // это пофиксилось само по себе когда я добавил параметризованный тест 
        softly.assertAll();

    }
    //Тест который запускает нашу программу для того чтобы протестировать активацию мобильного режима для UI
    //Desktop версия 90.3516px ширина кнопки
    //Mobile версии вычисляется через
    //в мобильной по данной формуле (ширина viewport-40)/2
    @ParameterizedTest
//    @CsvFileSource(emptyValue = "/Users/a11/IdeaProjects/bspb/src/test/java/resolution.csv",numLinesToSkip = 1)
    @CsvSource({
            "1400, 1920, false",
            "1023, 1920, true"

    })
    void TestScreenSize(int width,int height,boolean mobileExpected){
        NavigationPage navigationPage = new NavigationPage(driver);
        navigationPage.openMainPage();
        driver.manage().window().setSize(new Dimension(width,height));
        driver.navigate().refresh();
        double ElementWidth = navigationPage.getWidthByLink("IOS");
        boolean checkDesktopMode =ElementWidth < 100;
        if (mobileExpected) {
            assertFalse(checkDesktopMode);
        } else {
            assertTrue(checkDesktopMode);
        }
    }
    @Test
    public void testCards() throws Exception {
        NavigationPage navigationPage = new NavigationPage(driver);
        navigationPage.openMainPage();
        navigationPage.clickMenuItem("Дебетовые карты");
        driver.get("https://www.bspb.ru/retail/cards/debit");
        driver.findElement(By.linkText("Дебетовые карты")).click();
        driver.findElement(By.xpath("//div[@id='app-wrapper']/main/div/div[4]/div/div/div[2]/div")).click();
    }
    @Test
    //Проверка отклика на нажатие кнопки бургера
    public void testBurger() throws Exception {
        driver.manage().window().setSize(new Dimension(1000,1000));
        NavigationPage navigationPage = new NavigationPage(driver);
        navigationPage.openMainPage();
        // TODO:сделать xpath лучше
        navigationPage.ClickByXpath("(.//*[normalize-space(text()) and normalize-space(.)='Частным клиентам'])[1]/preceding::*[name()='svg'][1]");
        navigationPage.ClickByXpath("//button[@id='accordion-button-:r1i:']/p");
        navigationPage.ClickByXpath("//div[@id='accordion-panel-:r1i:']/a/p");
    }



}




