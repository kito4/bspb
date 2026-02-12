import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

public class Navigation extends BaseTest{
    private boolean acceptNextAlert = true;
    private StringBuffer verificationErrors = new StringBuffer();

    @Test
    void testMainMenuNavigation() {
        NavigationPage navigationPage = new NavigationPage(driver);
        for (String item : NavigationPage.MENU_ITEMS) {
            navigationPage.open();
            navigationPage.clickMenuItem(item);
            //System.out.println(item); debug
            String actualColor = navigationPage.getMenuItemColor(item);
            Assertions.assertEquals("rgba(21, 33, 38, 1)", actualColor);
        }
    }

}
