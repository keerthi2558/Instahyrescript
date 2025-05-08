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
		driver.get("https://www.instahyre.com/");

		boolean loggedInWithCookies = true;

		File file = new File(cookiesFile);
		if (file.exists()) {
			try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
				@SuppressWarnings("unchecked")
				Set<Cookie> cookies = (Set<Cookie>) ois.readObject();
				for (Cookie cookie : cookies) {
					if (cookie.getExpiry() == null || cookie.getExpiry().after(new Date())) {
						driver.manage().addCookie(cookie);
					}
				}

				driver.navigate().refresh();
				Thread.sleep(3000);

				if (driver.getCurrentUrl().contains("dashboard") || driver.getPageSource().contains("My Profile")) {
					System.out.println("Logged in using cookies.");
					loggedInWithCookies = true;
					return;
				}
			} catch (Exception e) {
				System.out.println("Failed to load cookies. Trying manual login...");
			}
		}

		// Manual login (when cookies fail or don't exist)
		if (!loggedInWithCookies) {
			try {
				System.out.println("Performing manual login...");
				driver.get("https://www.instahyre.com/");
				driver.findElement(By.id("nav-user-login")).click();

				wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email")));
				driver.findElement(By.id("email")).sendKeys(email);
				driver.findElement(By.id("password")).sendKeys(password);
				driver.findElement(By.xpath("(//button[@type='submit'])[1]")).click();

				wait.until(ExpectedConditions.urlContains("candidate/opportunities/?matching=true"));

				Set<Cookie> cookies = driver.manage().getCookies();
				try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(cookiesFile))) {
					oos.writeObject(cookies);
				}

				System.out.println("Manual login successful. Cookies saved.");
			} catch (Exception e) {
				System.out.println("Manual login failed.");
				throw e;
			}
		}
	}

	public static void main(String[] args) throws Exception {
		WebDriver driver = new ChromeDriver();
		try {
			login(driver);
			Thread.sleep(5000);

			// Check for the presence of the "interested" button before clicking
			List<WebElement> interestedButtons = driver.findElements(By.xpath("//button[@id='interested-btn']"));
			if (interestedButtons.isEmpty()) {
				System.out.println("No 'Interested' button found. Possibly no matching jobs.");
				return; // Exit early
			} else {
				interestedButtons.get(0).click();
				Thread.sleep(5000);
				applyJobs(driver);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			driver.quit();
		}
	}

	private static void applyJobs(WebDriver driver) throws Exception {
		System.out.println("Started applying");

		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

		while (true) {
			// Check if there are no more matching jobs
			if (isNoMatchingJobs(driver)) {
				System.out.println("No matching jobs found. Stopping.");
				break;
			}

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
						WebElement applyButton = wait.until(ExpectedConditions
								.elementToBeClickable(By.xpath("//button[contains(text(),'Apply')]")));
						((JavascriptExecutor) driver).executeScript("arguments[0].click();", applyButton);

						try {
							WebElement popup = wait
									.until(ExpectedConditions.visibilityOfElementLocated(By.id("jobSuggestionPopup")));
							if (popup.isDisplayed()) {
								System.out.println("Pop-up detected!");
								driver.findElement(By.xpath("//button[text()='Apply']")).click();
								System.out.println("Pop-up closed successfully!");
							}
						} catch (TimeoutException te) {
							System.out.println("No pop-up appeared.");
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

			Thread.sleep(5000); // Wait before moving to next job
		}
	}

	private static boolean isNoMatchingJobs(WebDriver driver) {
		try {
			List<WebElement> noJobsMessages = driver
					.findElements(By.xpath("//h6[contains(text(),'no matching opportunities found')]"));
			return !noJobsMessages.isEmpty() && noJobsMessages.get(0).isDisplayed();
		} catch (Exception e) {
			return false;
		}
	}

}
