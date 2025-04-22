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

public class InstahyreSingleJob2 {

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

	public static void main(String[] args) throws Exception {
		WebDriver driver = new ChromeDriver();
		try {
			login(driver);
			Thread.sleep(5000);
			driver.findElement(By.xpath("(//button[@id='interested-btn'])[1]")).click();
			Thread.sleep(5000);
			applyJobs(driver);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			driver.quit();
		}
	}

	private static void applyJobs(WebDriver driver) throws Exception {
		System.out.println("Started applying");
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

		try {
			WebElement experienceElement = wait.until(ExpectedConditions
					.visibilityOfElementLocated(By.xpath("//span[@class='experience ng-binding ng-scope']")));
			String experienceText = experienceElement.getText().trim();

			Pattern pattern = Pattern.compile("(\\d+)\\s*-\\s*(\\d+)");
			Matcher matcher = pattern.matcher(experienceText);

			System.out.println("Found experience = " + experienceText);

			if (matcher.find()) {
				int minExp = Integer.parseInt(matcher.group(1));
				int maxExp = Integer.parseInt(matcher.group(2));
				String skills = wait
						.until(ExpectedConditions
								.visibilityOfElementLocated(By.xpath("//div[contains(@class,'skills-container')]")))
						.getText();

				Set<String> skillsSet = new HashSet<>(Arrays.asList(skills.split("\\n")));
				if (minExp <= 5 && maxExp >= 5 && skillsSet.contains("Java")) {
					System.out.println("Skill matches. Applying...");
					WebElement applyButton = wait.until(
							ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(),'Apply')]")));
					((JavascriptExecutor) driver).executeScript("arguments[0].click();", applyButton);

					// WebDriverWait wait2 = new WebDriverWait(driver, Duration.ofSeconds(5));

					// Locate the pop-up using its unique ID or class
					WebElement popup = null;
					try {
						popup = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("jobSuggestionPopup")));
					} catch (Exception e) {
						System.out.println("No pop-up appeared.");
					}

					// If pop-up is present, close it
					if (popup != null && popup.isDisplayed()) {
						System.out.println("Pop-up detected!");

						// Click the close button inside the pop-up
						driver.findElement(By.xpath("//button[text()='Close']")).click();

						System.out.println("Pop-up closed successfully!");
					}
				} else {
					System.out.println("Not interested.");
					WebElement notInterestedButton = wait.until(ExpectedConditions
							.elementToBeClickable(By.xpath("//button[contains(text(),'Not interested')]")));
					((JavascriptExecutor) driver).executeScript("arguments[0].click();", notInterestedButton);
				}
				System.out.println(skills);
			}
		} catch (NoSuchElementException | TimeoutException e) {
			System.out.println("Could not find job application elements: " + e.getMessage());
		}

		Thread.sleep(5000);
		applyJobs(driver);

	}
}
