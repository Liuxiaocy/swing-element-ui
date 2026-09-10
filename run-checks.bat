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

echo [ 1/56] Checking Easing...
"%JRUN%" -ea -cp out org.swelement.core.Easing || set /a FAILED+=1
set /a TOTAL+=1

echo [ 2/56] Checking ElementTheme...
"%JRUN%" -ea -cp out org.swelement.core.ElementTheme || set /a FAILED+=1
set /a TOTAL+=1

echo [ 3/56] Checking ThemeManager...
"%JRUN%" -ea -cp out org.swelement.core.theme.ThemeManager || set /a FAILED+=1
set /a TOTAL+=1

echo [ 4/56] Checking ElementLightTheme...
"%JRUN%" -ea -cp out org.swelement.core.theme.ElementLightTheme || set /a FAILED+=1
set /a TOTAL+=1

echo [ 5/56] Checking AnimationManager...
"%JRUN%" -ea -cp out org.swelement.core.AnimationManager || set /a FAILED+=1
set /a TOTAL+=1

echo [ 6/56] Checking SelfCheckBase...
"%JRUN%" -ea -cp out org.swelement.core.SelfCheckBase || set /a FAILED+=1
set /a TOTAL+=1

echo [ 7/56] Checking PaintingHelper...
"%JRUN%" -ea -cp out org.swelement.framework.util.PaintingHelper || set /a FAILED+=1
set /a TOTAL+=1

echo [ 8/56] Checking AstCloseButton...
"%JRUN%" -ea -cp out org.swelement.ui.AstCloseButton || set /a FAILED+=1
set /a TOTAL+=1

echo [ 9/56] Checking AstTag...
"%JRUN%" -ea -cp out org.swelement.ui.AstTag || set /a FAILED+=1
set /a TOTAL+=1

echo [10/56] Checking AstAlert...
"%JRUN%" -ea -cp out org.swelement.ui.AstAlert || set /a FAILED+=1
set /a TOTAL+=1

echo [11/56] Checking AstSwitch...
"%JRUN%" -ea -cp out org.swelement.ui.AstSwitch || set /a FAILED+=1
set /a TOTAL+=1

echo [12/56] Checking AstRadio...
"%JRUN%" -ea -cp out org.swelement.ui.AstRadio || set /a FAILED+=1
set /a TOTAL+=1

echo [13/56] Checking AstCheckbox...
"%JRUN%" -ea -cp out org.swelement.ui.AstCheckbox || set /a FAILED+=1
set /a TOTAL+=1

echo [14/56] Checking AstButton...
"%JRUN%" -ea -cp out org.swelement.ui.AstButton || set /a FAILED+=1
set /a TOTAL+=1

echo [15/56] Checking AstTextArea...
"%JRUN%" -ea -cp out org.swelement.ui.AstTextArea || set /a FAILED+=1
set /a TOTAL+=1

echo [16/56] Checking AstInput...
"%JRUN%" -ea -cp out org.swelement.ui.AstInput || set /a FAILED+=1
set /a TOTAL+=1

echo [17/56] Checking AstInputNumber...
"%JRUN%" -ea -cp out org.swelement.ui.AstInputNumber || set /a FAILED+=1
set /a TOTAL+=1

echo [18/56] Checking AstSelect...
"%JRUN%" -ea -cp out org.swelement.ui.AstSelect || set /a FAILED+=1
set /a TOTAL+=1

echo [19/56] Checking AstDatePicker...
"%JRUN%" -ea -cp out org.swelement.ui.AstDatePicker || set /a FAILED+=1
set /a TOTAL+=1

echo [20/56] Checking AstSteps...
"%JRUN%" -ea -cp out org.swelement.ui.AstSteps || set /a FAILED+=1
set /a TOTAL+=1

echo [21/56] Checking AstBreadcrumb...
"%JRUN%" -ea -cp out org.swelement.ui.AstBreadcrumb || set /a FAILED+=1
set /a TOTAL+=1

echo [22/56] Checking AstTabs...
"%JRUN%" -ea -cp out org.swelement.ui.AstTabs || set /a FAILED+=1
set /a TOTAL+=1

echo [23/56] Checking AstPagination...
"%JRUN%" -ea -cp out org.swelement.ui.AstPagination || set /a FAILED+=1
set /a TOTAL+=1

echo [24/56] Checking AstCollapse...
"%JRUN%" -ea -cp out org.swelement.ui.AstCollapse || set /a FAILED+=1
set /a TOTAL+=1

echo [25/56] Checking AstLoading...
"%JRUN%" -ea -cp out org.swelement.ui.AstLoading || set /a FAILED+=1
set /a TOTAL+=1

echo [26/56] Checking AstTooltip...
"%JRUN%" -ea -cp out org.swelement.ui.AstTooltip || set /a FAILED+=1
set /a TOTAL+=1

echo [27/56] Checking AstPopover...
"%JRUN%" -ea -cp out org.swelement.ui.AstPopover || set /a FAILED+=1
set /a TOTAL+=1

echo [28/56] Checking AstMessage...
"%JRUN%" -ea -cp out org.swelement.ui.AstMessage || set /a FAILED+=1
set /a TOTAL+=1

echo [29/56] Checking AstDialog...
"%JRUN%" -ea -cp out org.swelement.ui.AstDialog || set /a FAILED+=1
set /a TOTAL+=1

echo [30/56] Checking AstIcon...
"%JRUN%" -ea -cp out org.swelement.ui.AstIcon || set /a FAILED+=1
set /a TOTAL+=1

echo [31/56] Checking AstAvatar...
"%JRUN%" -ea -cp out org.swelement.ui.AstAvatar || set /a FAILED+=1
set /a TOTAL+=1

echo [32/56] Checking AstBadge...
"%JRUN%" -ea -cp out org.swelement.ui.AstBadge || set /a FAILED+=1
set /a TOTAL+=1

echo [33/56] Checking AstProgress...
"%JRUN%" -ea -cp out org.swelement.ui.AstProgress || set /a FAILED+=1
set /a TOTAL+=1

echo [34/56] Checking AstDivider...
"%JRUN%" -ea -cp out org.swelement.ui.AstDivider || set /a FAILED+=1
set /a TOTAL+=1

echo [35/56] Checking AstTimeline...
"%JRUN%" -ea -cp out org.swelement.ui.AstTimeline || set /a FAILED+=1
set /a TOTAL+=1

echo [36/56] Checking AstCard...
"%JRUN%" -ea -cp out org.swelement.ui.AstCard || set /a FAILED+=1
set /a TOTAL+=1

echo [37/56] Checking AstCalendar...
"%JRUN%" -ea -cp out org.swelement.ui.AstCalendar || set /a FAILED+=1
set /a TOTAL+=1

echo [38/56] Checking AstCarousel...
"%JRUN%" -ea -cp out org.swelement.ui.AstCarousel || set /a FAILED+=1
set /a TOTAL+=1

echo [39/56] Checking AstTree...
"%JRUN%" -ea -cp out org.swelement.ui.AstTree || set /a FAILED+=1
set /a TOTAL+=1

echo [40/56] Checking AstTable...
"%JRUN%" -ea -cp out org.swelement.ui.AstTable || set /a FAILED+=1
set /a TOTAL+=1

echo [41/56] Checking AstContainer...
"%JRUN%" -ea -cp out org.swelement.ui.AstContainer || set /a FAILED+=1
set /a TOTAL+=1

echo [42/56] Checking AstSlider...
"%JRUN%" -ea -cp out org.swelement.ui.AstSlider || set /a FAILED+=1
set /a TOTAL+=1

echo [43/56] Checking AstRate...
"%JRUN%" -ea -cp out org.swelement.ui.AstRate || set /a FAILED+=1
set /a TOTAL+=1

echo [44/56] Checking AstMenu...
"%JRUN%" -ea -cp out org.swelement.ui.AstMenu || set /a FAILED+=1
set /a TOTAL+=1

echo [45/56] Checking AstForm...
"%JRUN%" -ea -cp out org.swelement.ui.AstForm || set /a FAILED+=1
set /a TOTAL+=1

echo [46/56] Checking AstDropdown...
"%JRUN%" -ea -cp out org.swelement.ui.AstDropdown || set /a FAILED+=1
set /a TOTAL+=1

echo [47/56] Checking AstTimePicker...
"%JRUN%" -ea -cp out org.swelement.ui.AstTimePicker || set /a FAILED+=1
set /a TOTAL+=1

echo [48/56] Checking AstCascader...
"%JRUN%" -ea -cp out org.swelement.ui.AstCascader || set /a FAILED+=1
set /a TOTAL+=1

echo [49/56] Checking AstTransfer...
"%JRUN%" -ea -cp out org.swelement.ui.AstTransfer || set /a FAILED+=1
set /a TOTAL+=1

echo [50/56] Checking AstMessageBox...
"%JRUN%" -ea -cp out org.swelement.ui.AstMessageBox || set /a FAILED+=1
set /a TOTAL+=1

echo [51/56] Checking AstDrawer...
"%JRUN%" -ea -cp out org.swelement.ui.AstDrawer || set /a FAILED+=1
set /a TOTAL+=1

echo [52/56] Checking AstEmpty...
"%JRUN%" -ea -cp out org.swelement.ui.AstEmpty || set /a FAILED+=1
set /a TOTAL+=1

echo [53/56] Checking AstNotification...
"%JRUN%" -ea -cp out org.swelement.ui.AstNotification || set /a FAILED+=1
set /a TOTAL+=1

echo [54/56] Checking AstTabsDemo...
"%JRUN%" -ea -cp out org.swelement.demo.AstTabsDemo --selfcheck || set /a FAILED+=1
set /a TOTAL+=1

echo [55/56] Checking AstMenuDemo...
"%JRUN%" -ea -cp out org.swelement.demo.AstMenuDemo --selfcheck || set /a FAILED+=1
set /a TOTAL+=1

echo [56/56] Checking docs consistency (snippets / links / commands)...
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
