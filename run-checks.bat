@echo off
setlocal enabledelayedexpansion
rem ---- Use Java 8 if available (target JDK 1.8) ----
set "JAVA8=C:\Program Files\Java\jdk1.8.0_311"
set "JAVAC=javac"
set "JRUN=java"
if exist "%JAVA8%\bin\javac.exe" (
  set "JAVAC=%JAVA8%\bin\javac.exe"
) else (
  echo WARN: 未找到 JDK 1.8，回退使用 PATH 上的 javac/java
)
"%JRUN%" -version >nul 2>nul || (echo ERROR: java not found & exit /b 1)

echo ========================================
echo   Swing Element UI - Self-Check Suite
echo ========================================
echo.

set FAILED=0
set TOTAL=0

echo [ 1/59] Checking Easing...
"%JRUN%" -ea -cp out org.swelement.core.Easing || set /a FAILED+=1
set /a TOTAL+=1

echo [ 2/59] Checking ElementTheme...
"%JRUN%" -ea -cp out org.swelement.core.ElementTheme || set /a FAILED+=1
set /a TOTAL+=1

echo [ 3/59] Checking ThemeManager...
"%JRUN%" -ea -cp out org.swelement.core.theme.ThemeManager || set /a FAILED+=1
set /a TOTAL+=1

echo [ 4/59] Checking ElementLightTheme...
"%JRUN%" -ea -cp out org.swelement.core.theme.ElementLightTheme || set /a FAILED+=1
set /a TOTAL+=1

echo [ 5/59] Checking AnimationManager...
"%JRUN%" -ea -cp out org.swelement.core.AnimationManager || set /a FAILED+=1
set /a TOTAL+=1

echo [ 6/59] Checking SelfCheckBase...
"%JRUN%" -ea -cp out org.swelement.core.SelfCheckBase || set /a FAILED+=1
set /a TOTAL+=1

echo [ 7/59] Checking PaintingHelper...
"%JRUN%" -ea -cp out org.swelement.framework.util.PaintingHelper || set /a FAILED+=1
set /a TOTAL+=1

echo [ 8/59] Checking AstCloseButton...
"%JRUN%" -ea -cp out org.swelement.ui.AstCloseButton || set /a FAILED+=1
set /a TOTAL+=1

echo [ 9/59] Checking AstTag...
"%JRUN%" -ea -cp out org.swelement.ui.AstTag || set /a FAILED+=1
set /a TOTAL+=1

echo [10/59] Checking AstAlert...
"%JRUN%" -ea -cp out org.swelement.ui.AstAlert || set /a FAILED+=1
set /a TOTAL+=1

echo [11/59] Checking AstSwitch...
"%JRUN%" -ea -cp out org.swelement.ui.AstSwitch || set /a FAILED+=1
set /a TOTAL+=1

echo [12/59] Checking AstRadio...
"%JRUN%" -ea -cp out org.swelement.ui.AstRadio || set /a FAILED+=1
set /a TOTAL+=1

echo [13/59] Checking AstCheckbox...
"%JRUN%" -ea -cp out org.swelement.ui.AstCheckbox || set /a FAILED+=1
set /a TOTAL+=1

echo [14/59] Checking AstButton...
"%JRUN%" -ea -cp out org.swelement.ui.AstButton || set /a FAILED+=1
set /a TOTAL+=1

echo [15/59] Checking AstTextArea...
"%JRUN%" -ea -cp out org.swelement.ui.AstTextArea || set /a FAILED+=1
set /a TOTAL+=1

echo [16/59] Checking AstInput...
"%JRUN%" -ea -cp out org.swelement.ui.AstInput || set /a FAILED+=1
set /a TOTAL+=1

echo [17/59] Checking AstInputNumber...
"%JRUN%" -ea -cp out org.swelement.ui.AstInputNumber || set /a FAILED+=1
set /a TOTAL+=1

echo [18/59] Checking AstSelect...
"%JRUN%" -ea -cp out org.swelement.ui.AstSelect || set /a FAILED+=1
set /a TOTAL+=1

echo [19/59] Checking AstDatePicker...
"%JRUN%" -ea -cp out org.swelement.ui.AstDatePicker || set /a FAILED+=1
set /a TOTAL+=1

echo [20/59] Checking AstSteps...
"%JRUN%" -ea -cp out org.swelement.ui.AstSteps || set /a FAILED+=1
set /a TOTAL+=1

echo [21/59] Checking AstBreadcrumb...
"%JRUN%" -ea -cp out org.swelement.ui.AstBreadcrumb || set /a FAILED+=1
set /a TOTAL+=1

echo [22/59] Checking AstTabs...
"%JRUN%" -ea -cp out org.swelement.ui.AstTabs || set /a FAILED+=1
set /a TOTAL+=1

echo [23/59] Checking AstPagination...
"%JRUN%" -ea -cp out org.swelement.ui.AstPagination || set /a FAILED+=1
set /a TOTAL+=1

echo [24/59] Checking AstCollapse...
"%JRUN%" -ea -cp out org.swelement.ui.AstCollapse || set /a FAILED+=1
set /a TOTAL+=1

echo [25/59] Checking AstLoading...
"%JRUN%" -ea -cp out org.swelement.ui.AstLoading || set /a FAILED+=1
set /a TOTAL+=1

echo [26/59] Checking AstTooltip...
"%JRUN%" -ea -cp out org.swelement.ui.AstTooltip || set /a FAILED+=1
set /a TOTAL+=1

echo [27/59] Checking AstPopover...
"%JRUN%" -ea -cp out org.swelement.ui.AstPopover || set /a FAILED+=1
set /a TOTAL+=1

echo [28/59] Checking AstMessage...
"%JRUN%" -ea -cp out org.swelement.ui.AstMessage || set /a FAILED+=1
set /a TOTAL+=1

echo [29/59] Checking AstDialog...
"%JRUN%" -ea -cp out org.swelement.ui.AstDialog || set /a FAILED+=1
set /a TOTAL+=1

echo [30/59] Checking AstIcon...
"%JRUN%" -ea -cp out org.swelement.ui.AstIcon || set /a FAILED+=1
set /a TOTAL+=1

echo [31/59] Checking AstAvatar...
"%JRUN%" -ea -cp out org.swelement.ui.AstAvatar || set /a FAILED+=1
set /a TOTAL+=1

echo [32/59] Checking AstBadge...
"%JRUN%" -ea -cp out org.swelement.ui.AstBadge || set /a FAILED+=1
set /a TOTAL+=1

echo [33/59] Checking AstProgress...
"%JRUN%" -ea -cp out org.swelement.ui.AstProgress || set /a FAILED+=1
set /a TOTAL+=1

echo [34/59] Checking AstDivider...
"%JRUN%" -ea -cp out org.swelement.ui.AstDivider || set /a FAILED+=1
set /a TOTAL+=1

echo [35/59] Checking AstTimeline...
"%JRUN%" -ea -cp out org.swelement.ui.AstTimeline || set /a FAILED+=1
set /a TOTAL+=1

echo [36/59] Checking AstCard...
"%JRUN%" -ea -cp out org.swelement.ui.AstCard || set /a FAILED+=1
set /a TOTAL+=1

echo [37/59] Checking AstCalendar...
"%JRUN%" -ea -cp out org.swelement.ui.AstCalendar || set /a FAILED+=1
set /a TOTAL+=1

echo [38/59] Checking AstCarousel...
"%JRUN%" -ea -cp out org.swelement.ui.AstCarousel || set /a FAILED+=1
set /a TOTAL+=1

echo [39/59] Checking AstTree...
"%JRUN%" -ea -cp out org.swelement.ui.AstTree || set /a FAILED+=1
set /a TOTAL+=1

echo [40/59] Checking AstTable...
"%JRUN%" -ea -cp out org.swelement.ui.AstTable || set /a FAILED+=1
set /a TOTAL+=1

echo [41/59] Checking AstContainer...
"%JRUN%" -ea -cp out org.swelement.ui.AstContainer || set /a FAILED+=1
set /a TOTAL+=1

echo [42/59] Checking AstSlider...
"%JRUN%" -ea -cp out org.swelement.ui.AstSlider || set /a FAILED+=1
set /a TOTAL+=1

echo [43/59] Checking AstRate...
"%JRUN%" -ea -cp out org.swelement.ui.AstRate || set /a FAILED+=1
set /a TOTAL+=1

echo [44/59] Checking AstMenu...
"%JRUN%" -ea -cp out org.swelement.ui.AstMenu || set /a FAILED+=1
set /a TOTAL+=1

echo [45/59] Checking AstForm...
"%JRUN%" -ea -cp out org.swelement.ui.AstForm || set /a FAILED+=1
set /a TOTAL+=1

echo [46/59] Checking AstDropdown...
"%JRUN%" -ea -cp out org.swelement.ui.AstDropdown || set /a FAILED+=1
set /a TOTAL+=1

echo [47/59] Checking AstTimePicker...
"%JRUN%" -ea -cp out org.swelement.ui.AstTimePicker || set /a FAILED+=1
set /a TOTAL+=1

echo [48/59] Checking AstCascader...
"%JRUN%" -ea -cp out org.swelement.ui.AstCascader || set /a FAILED+=1
set /a TOTAL+=1

echo [49/59] Checking AstTransfer...
"%JRUN%" -ea -cp out org.swelement.ui.AstTransfer || set /a FAILED+=1
set /a TOTAL+=1

echo [50/59] Checking AstMessageBox...
"%JRUN%" -ea -cp out org.swelement.ui.AstMessageBox || set /a FAILED+=1
set /a TOTAL+=1

echo [51/59] Checking AstDrawer...
"%JRUN%" -ea -cp out org.swelement.ui.AstDrawer || set /a FAILED+=1
set /a TOTAL+=1

echo [52/59] Checking AstEmpty...
"%JRUN%" -ea -cp out org.swelement.ui.AstEmpty || set /a FAILED+=1
set /a TOTAL+=1

echo [53/59] Checking AstNotification...
"%JRUN%" -ea -cp out org.swelement.ui.AstNotification || set /a FAILED+=1
set /a TOTAL+=1

echo [54/59] Checking AstTabsDemo...
"%JRUN%" -ea -cp out org.swelement.demo.AstTabsDemo --selfcheck || set /a FAILED+=1
set /a TOTAL+=1

echo [55/59] Checking AstMenuDemo...
"%JRUN%" -ea -cp out org.swelement.demo.AstMenuDemo --selfcheck || set /a FAILED+=1
set /a TOTAL+=1

echo [56/59] Checking AstStepsDemo...
"%JRUN%" -ea -cp out org.swelement.demo.AstStepsDemo --selfcheck || set /a FAILED+=1
set /a TOTAL+=1

echo [57/59] Checking AstTimelineDemo...
"%JRUN%" -ea -cp out org.swelement.demo.AstTimelineDemo --selfcheck || set /a FAILED+=1
set /a TOTAL+=1

echo [58/59] Checking AstRadioDemo...
"%JRUN%" -ea -cp out org.swelement.demo.AstRadioDemo --selfcheck || set /a FAILED+=1
set /a TOTAL+=1

echo [59/59] Checking docs consistency (snippets / links / commands)...
"%JAVAC%" -encoding UTF-8 -cp out -d out tools\DocSnippetCheck.java >nul 2>nul
"%JRUN%" -cp out DocSnippetCheck . || set /a FAILED+=1
set /a TOTAL+=1

echo.
echo ========================================
if %FAILED%==0 (
    echo   ALL %TOTAL% CHECKS PASSED
) else (
    echo   %FAILED% of %TOTAL% CHECKS FAILED
)
echo ========================================
if %FAILED%==0 exit /b 0
exit /b 1
