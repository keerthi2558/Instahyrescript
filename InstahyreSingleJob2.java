import java.io.*;
import java.time.Duration;
import java.util.*;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class InstahyreAutoApply {

	public static void login(WebDriver driver) throws Exception {
		String email = "swamymushini@gmail.com";
		String password = "ieNEWSS3**20242025";
		String cookiesFile = "cookies.data";

		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		driver.manage().window().maximize();
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
		File file = new File(cookiesFile);
		boolean loggedInWithCookies = false;

		driver.get("https://www.instahyre.com/");

		try {
			if (file.exists()) {
				// Load cookies from file
				try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cookiesFile))) {
					@SuppressWarnings("unchecked")
					Set<Cookie> cookies = (Set<Cookie>) ois.readObject();

					for (Cookie cookie : cookies) {
						// Skip expired cookies
						if (cookie.getExpiry() == null || cookie.getExpiry().after(new Date())) {
							driver.manage().addCookie(cookie);
						}
					}

					driver.navigate().refresh(); // Apply cookies
					Thread.sleep(3000); // Wait a bit after refreshing

					// Check if logged in successfully (use element unique to logged-in state)
					if (driver.getCurrentUrl().contains("dashboard") || driver.getPageSource().contains("My Profile")) {
						System.out.println("Logged in using cookies.");
						loggedInWithCookies = true;
					}
				}
			}
			else if (!loggedInWithCookies) {
				System.out.println("Performing manual login...");
				driver.get("https://www.instahyre.com/");
				driver.findElement(By.id("nav-user-login")).click();

				wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email")));
				driver.findElement(By.id("email")).sendKeys(email);
				driver.findElement(By.id("password")).sendKeys(password);
				driver.findElement(By.xpath("(//button[@type='submit'])[1]")).click();

				// Wait for dashboard or logged-in element
				wait.until(ExpectedConditions.urlContains("candidate/opportunities/?matching=true"));

				Set<Cookie> cookies = driver.manage().getCookies();

				try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(cookiesFile))) {
					oos.writeObject(cookies);
				}

				System.out.println("Logged in and cookies saved.");
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Login failed.");
		}
	}

	public static void main(String[] args) {
        WebDriver driver = new ChromeDriver();

        try {
            login(driver);
            Thread.sleep(5000);

            // check "no matching jobs" BEFORE starting apply
            if (isNoMatchingJobs(driver)) {
                System.out.println("No matching jobs found. Exiting...");
                return;
            }

            // Click first "interested" if available
            List<WebElement> interestedBtns = driver.findElements(By.id("interested-btn"));
            if (!interestedBtns.isEmpty()) {
                interestedBtns.get(0).click();
                Thread.sleep(3000);
            }

            applyJobs(driver);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
            System.out.println("Driver closed.");
        }
    }

    private static boolean isNoMatchingJobs(WebDriver driver) {
        try {
            List<WebElement> noJobsMessages = driver.findElements(
                    By.xpath("//h6[contains(text(),'no matching opportunities found')]"));
            return !noJobsMessages.isEmpty() && noJobsMessages.get(0).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    private static void applyJobs(WebDriver driver) throws Exception {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        System.out.println("Start applying jobs...");

        while (true) { 
            try {
                if (isNoMatchingJobs(driver)) {
                    System.out.println("No more matching jobs. Exiting apply loop...");
                    break;
                }

                WebElement experienceElement = wait.until(ExpectedConditions
                        .visibilityOfElementLocated(By.xpath("//span[contains(@class,'experience')]")));
                String experienceText = experienceElement.getText().trim();
                System.out.println("Experience found: " + experienceText);

                Pattern pattern = Pattern.compile("(\\d+)\\s*-\\s*(\\d+)");
                Matcher matcher = pattern.matcher(experienceText);

                if (matcher.find()) {
                    int minExp = Integer.parseInt(matcher.group(1));
                    int maxExp = Integer.parseInt(matcher.group(2));
                    String skills = wait.until(ExpectedConditions
                            .visibilityOfElementLocated(By.xpath("//div[contains(@class,'skills-container')]"))).getText();
                    Set<String> skillsSet = new HashSet<>(Arrays.asList(skills.split("\\n")));

                    if (minExp <= 5 && maxExp >= 5 && skillsSet.contains("Java")) {
                        System.out.println("Skills match. Applying...");
                        WebElement applyButton = wait.until(ExpectedConditions
                                .elementToBeClickable(By.xpath("//button[contains(text(),'Apply')]")));
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", applyButton);
                    } else {
                        System.out.println("Skills do not match. Skipping...");
                        WebElement notInterestedButton = wait.until(ExpectedConditions
                                .elementToBeClickable(By.xpath("//button[contains(text(),'Not interested')]")));
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", notInterestedButton);
                    }

                    // Handle popup if appears
                    handlePopup(driver);

                } else {
                    System.out.println("Could not parse experience.");
                }

            } catch (TimeoutException e) {
                System.out.println("Timeout waiting for elements. Skipping this job...");
            } catch (NoSuchElementException e) {
                System.out.println("No more jobs visible. Exiting...");
                break;
            }

            Thread.sleep(3000);
        }
    }

    private static void handlePopup(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(3));
            WebElement popup = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("jobSuggestionPopup")));
            if (popup.isDisplayed()) {
                System.out.println("Popup detected. Closing...");
                driver.findElement(By.xpath("//button[text()='Close']")).click();
            }
        } catch (TimeoutException e) {
            // No popup appeared
        }
    }
}
