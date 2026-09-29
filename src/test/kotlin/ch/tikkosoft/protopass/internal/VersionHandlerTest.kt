package ch.tikkosoft.protopass.internal

import ch.tikkosoft.protopass.model.ProjectProperties
import ch.tikkosoft.protopass.model.VersionStrategy
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Properties
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VersionHandlerTest {

    private lateinit var tempFile: File

    @BeforeTest
    fun setup() {
        tempFile = File.createTempFile("config", ".properties")
    }

    @AfterTest
    fun tearDown() {
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }

    @Test
    fun testIncrementVersionCodeOnSameDay() {
        // Arrange
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val currentVersionCode = "${today}00"
        
        val props = Properties()
        props.setProperty(ProjectProperties.VERSION_CODE, currentVersionCode)
        tempFile.writer().use { props.store(it, null) }

        val handler = VersionHandler()

        // Act
        val newVersionCode = handler.calculateVersionCode(tempFile)

        // Assert
        val expectedVersionCode = (currentVersionCode.toLong() + 1).toInt()
        assertEquals(expectedVersionCode, newVersionCode)
    }

    @Test
    fun testResetVersionCodeOnNewDay() {
        // Arrange
        val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val yesterdayVersionCode = "${yesterday}99"

        val props = Properties()
        props.setProperty(ProjectProperties.VERSION_CODE, yesterdayVersionCode)
        tempFile.writer().use { props.store(it, null) }

        val handler = VersionHandler()

        // Act
        val newVersionCode = handler.calculateVersionCode(tempFile)

        // Assert
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val expectedVersionCode = "${today}00".toInt()

        assertEquals(expectedVersionCode, newVersionCode)
    }
    
    @Test
    fun testHandleMissingFile() {
        // Arrange
        if (tempFile.exists()) {
            tempFile.delete()
        }
        val handler = VersionHandler()
        
        // Act
        val newVersionCode = handler.calculateVersionCode(tempFile)
        
        // Assert
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val expectedVersionCode = "${today}00".toInt()
        
        assertEquals(expectedVersionCode, newVersionCode)
        assertTrue(tempFile.exists(), "The configuration file should be created if it was missing")
    }

    @Test
    fun testSimpleIncrementStrategy() {
        // Arrange
        val currentVersionCode = "100"
        
        val props = Properties()
        props.setProperty(ProjectProperties.VERSION_CODE, currentVersionCode)
        tempFile.writer().use { props.store(it, null) }

        val handler = VersionHandler()

        // Act
        val newVersionCode = handler.calculateVersionCode(tempFile, VersionStrategy.NUMBER)

        // Assert
        assertEquals(101, newVersionCode)
    }

    @Test
    fun testDateCodeFallbackIncrement() {
        // Arrange
        val futureVersionCode = "2099010100"

        val props = Properties()
        props.setProperty(ProjectProperties.VERSION_CODE, futureVersionCode)
        tempFile.writer().use { props.store(it, null) }

        val handler = VersionHandler()

        // Act
        val newVersionCode = handler.calculateVersionCode(tempFile, VersionStrategy.DATE_CODE)

        // Assert
        assertEquals(2099010101, newVersionCode)
    }

    @Test
    fun testInvalidVersionCodeFormat() {
        // Arrange
        val props = Properties()
        props.setProperty(ProjectProperties.VERSION_CODE, "invalid-code")
        tempFile.writer().use { props.store(it, null) }

        val handler = VersionHandler()

        // Act
        val newVersionCode = handler.calculateVersionCode(tempFile)

        // Assert
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val expectedVersionCode = "${today}00".toInt()

        assertEquals(expectedVersionCode, newVersionCode)
    }

    @Test
    fun testNumberStrategyMissingFile() {
        // Arrange
        if (tempFile.exists()) {
            tempFile.delete()
        }
        val handler = VersionHandler()

        // Act
        val newVersionCode = handler.calculateVersionCode(tempFile, VersionStrategy.NUMBER)

        // Assert
        // Defaults to 2024010100L + 1
        assertEquals(2024010101, newVersionCode)
    }
}
