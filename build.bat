@echo off
setlocal enabledelayedexpansion
rem ---- 只允许原生 Java 8 编译/运行（项目目标 JDK 1.8）----
set "JAVA8=C:\Program Files\Java\jdk1.8.0_311"
set "JAVAC=javac"
set "JRUN=java"
if exist "%JAVA8%\bin\javac.exe" (
  set "JAVAC=%JAVA8%\bin\javac.exe"
  set "JRUN=%JAVA8%\bin\java.exe"
) else (
  echo WARN: 未找到 JDK 1.8，回退使用 PATH 上的 javac/java
)
"%JAVAC%" -version >nul 2>nul || (echo ERROR: javac not found & exit /b 1)
if not exist out mkdir out

rem ---- 动态收集 src 下的所有 .java 源文件（避免手工清单遗漏/失效）----
set "SOURCES="
for /r src %%f in (*.java) do set "SOURCES=!SOURCES! "%%f""

rem JDK 1.8 直接编译即可（默认 -source/-target 8）；若回退到高版本 JDK 则用 --release 8 兜底
"%JAVAC%" -encoding UTF-8 -d out %SOURCES%
if errorlevel 1 (
  echo retry with --release 8
  "%JAVAC%" -encoding UTF-8 --release 8 -d out %SOURCES%
)
if errorlevel 1 (echo BUILD FAILED & exit /b 1)
echo BUILD OK

echo --- ThemeManager self-check ---
"%JRUN%" -ea -cp out org.swelement.core.theme.ThemeManager
if %ERRORLEVEL% NEQ 0 ( echo ThemeManager self-check FAILED & exit /b 1 )

echo --- ElementLightTheme self-check ---
"%JRUN%" -ea -cp out org.swelement.core.theme.ElementLightTheme
if %ERRORLEVEL% NEQ 0 ( echo ElementLightTheme self-check FAILED & exit /b 1 )

echo --- AnimationManager self-check ---
"%JRUN%" -ea -cp out org.swelement.core.AnimationManager
if %ERRORLEVEL% NEQ 0 ( echo AnimationManager self-check FAILED & exit /b 1 )

echo --- SelfCheckBase self-check ---
"%JRUN%" -ea -cp out org.swelement.core.SelfCheckBase
if %ERRORLEVEL% NEQ 0 ( echo SelfCheckBase self-check FAILED & exit /b 1 )

echo --- PaintingHelper self-check ---
"%JRUN%" -ea -cp out org.swelement.framework.util.PaintingHelper
if %ERRORLEVEL% NEQ 0 ( echo PaintingHelper self-check FAILED & exit /b 1 )

echo --- CloseButton self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstCloseButton
if %ERRORLEVEL% NEQ 0 ( echo CloseButton self-check FAILED & exit /b 1 )

echo --- Tag self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstTag
if %ERRORLEVEL% NEQ 0 ( echo Tag self-check FAILED & exit /b 1 )

echo --- Alert self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstAlert
if %ERRORLEVEL% NEQ 0 ( echo Alert self-check FAILED & exit /b 1 )

echo --- Input self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstInput
if %ERRORLEVEL% NEQ 0 ( echo Input self-check FAILED & exit /b 1 )

echo --- TextArea self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstTextArea
if %ERRORLEVEL% NEQ 0 ( echo TextArea self-check FAILED & exit /b 1 )

echo --- AstContainer self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstContainer
if %ERRORLEVEL% NEQ 0 ( echo AstContainer self-check FAILED & exit /b 1 )

echo --- AstAvatar self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstAvatar
if %ERRORLEVEL% NEQ 0 ( echo AstAvatar self-check FAILED & exit /b 1 )

echo --- AstCard self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstCard
if %ERRORLEVEL% NEQ 0 ( echo AstCard self-check FAILED & exit /b 1 )

echo --- AstLoading self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstLoading
if %ERRORLEVEL% NEQ 0 ( echo AstLoading self-check FAILED & exit /b 1 )

echo --- AstTooltip self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstTooltip
if %ERRORLEVEL% NEQ 0 ( echo AstTooltip self-check FAILED & exit /b 1 )

echo --- AstDropdown self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstDropdown
if %ERRORLEVEL% NEQ 0 ( echo AstDropdown self-check FAILED & exit /b 1 )

echo --- AstDialog self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstDialog
if %ERRORLEVEL% NEQ 0 ( echo AstDialog self-check FAILED & exit /b 1 )

echo --- AstMessageBox self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstMessageBox
if %ERRORLEVEL% NEQ 0 ( echo AstMessageBox self-check FAILED & exit /b 1 )

echo --- AstMessage self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstMessage
if %ERRORLEVEL% NEQ 0 ( echo AstMessage self-check FAILED & exit /b 1 )

echo --- AstCascader self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstCascader
if %ERRORLEVEL% NEQ 0 ( echo AstCascader self-check FAILED & exit /b 1 )

echo --- AstDatePicker self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstDatePicker
if %ERRORLEVEL% NEQ 0 ( echo AstDatePicker self-check FAILED & exit /b 1 )

echo --- AstForm self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstForm
if %ERRORLEVEL% NEQ 0 ( echo AstForm self-check FAILED & exit /b 1 )

echo --- AstTree self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstTree
if %ERRORLEVEL% NEQ 0 ( echo AstTree self-check FAILED & exit /b 1 )

echo --- AstTable self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstTable
if %ERRORLEVEL% NEQ 0 ( echo AstTable self-check FAILED & exit /b 1 )

echo --- AstTableDemo self-check ---
"%JRUN%" -ea -cp out org.swelement.demo.AstTableDemo --selfcheck
if %ERRORLEVEL% NEQ 0 ( echo AstTableDemo self-check FAILED & exit /b 1 )

echo --- AstDivider self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstDivider
if %ERRORLEVEL% NEQ 0 ( echo AstDivider self-check FAILED & exit /b 1 )

echo --- AstIcon self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstIcon
if %ERRORLEVEL% NEQ 0 ( echo AstIcon self-check FAILED & exit /b 1 )

echo --- AstRate self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstRate
if %ERRORLEVEL% NEQ 0 ( echo AstRate self-check FAILED & exit /b 1 )

echo --- AstBreadcrumb self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstBreadcrumb
if %ERRORLEVEL% NEQ 0 ( echo AstBreadcrumb self-check FAILED & exit /b 1 )

echo --- AstSteps self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstSteps
if %ERRORLEVEL% NEQ 0 ( echo AstSteps self-check FAILED & exit /b 1 )

echo --- AstCollapse self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstCollapse
if %ERRORLEVEL% NEQ 0 ( echo AstCollapse self-check FAILED & exit /b 1 )

echo --- AstInputNumber self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstInputNumber
if %ERRORLEVEL% NEQ 0 ( echo AstInputNumber self-check FAILED & exit /b 1 )

echo --- AstPopover self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstPopover
if %ERRORLEVEL% NEQ 0 ( echo AstPopover self-check FAILED & exit /b 1 )

echo --- AstDrawer self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstDrawer
if %ERRORLEVEL% NEQ 0 ( echo AstDrawer self-check FAILED & exit /b 1 )

echo --- AstTimePicker self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstTimePicker
if %ERRORLEVEL% NEQ 0 ( echo AstTimePicker self-check FAILED & exit /b 1 )

echo --- AstTransfer self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstTransfer
if %ERRORLEVEL% NEQ 0 ( echo AstTransfer self-check FAILED & exit /b 1 )

echo --- AstTimeline self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstTimeline
if %ERRORLEVEL% NEQ 0 ( echo AstTimeline self-check FAILED & exit /b 1 )

echo --- AstCalendar self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstCalendar
if %ERRORLEVEL% NEQ 0 ( echo AstCalendar self-check FAILED & exit /b 1 )

echo --- AstCarousel self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstCarousel
if %ERRORLEVEL% NEQ 0 ( echo AstCarousel self-check FAILED & exit /b 1 )

echo --- AstIconDemo self-check ---
"%JRUN%" -ea -cp out org.swelement.demo.AstIconDemo --selfcheck
if %ERRORLEVEL% NEQ 0 ( echo AstIconDemo self-check FAILED & exit /b 1 )

echo --- AstBadge self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstBadge
if %ERRORLEVEL% NEQ 0 ( echo AstBadge self-check FAILED & exit /b 1 )

echo --- AstSwitch self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstSwitch
if %ERRORLEVEL% NEQ 0 ( echo AstSwitch self-check FAILED & exit /b 1 )

echo --- AstRadio self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstRadio
if %ERRORLEVEL% NEQ 0 ( echo AstRadio self-check FAILED & exit /b 1 )

echo --- AstCheckbox self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstCheckbox
if %ERRORLEVEL% NEQ 0 ( echo AstCheckbox self-check FAILED & exit /b 1 )

echo --- AstButton self-check ---
"%JRUN%" -ea -cp out org.swelement.ui.AstButton
if %ERRORLEVEL% NEQ 0 ( echo AstButton self-check FAILED & exit /b 1 )
