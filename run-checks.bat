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

echo [ 1/62] Checking Easing...
"%JRUN%" -ea -cp out org.swelement.core.Easing || set /a FAILED+=1
set /a TOTAL+=1

echo [ 2/62] Checking ElementTheme...
"%JRUN%" -ea -cp out org.swelement.core.ElementTheme || set /a FAILED+=1
set /a TOTAL+=1

echo [ 3/62] Checking ThemeManager...
"%JRUN%" -ea -cp out org.swelement.core.theme.ThemeManager || set /a FAILED+=1
set /a TOTAL+=1

echo [ 4/62] Checking ElementLightTheme...
"%JRUN%" -ea -cp out org.swelement.core.theme.ElementLightTheme || set /a FAILED+=1
set /a TOTAL+=1

echo [ 5/62] Checking AnimationManager...
"%JRUN%" -ea -cp out org.swelement.core.AnimationManager || set /a FAILED+=1
set /a TOTAL+=1

echo [ 6/62] Checking SelfCheckBase...
"%JRUN%" -ea -cp out org.swelement.core.SelfCheckBase || set /a FAILED+=1
set /a TOTAL+=1

echo [ 7/62] Checking PaintingHelper...
"%JRUN%" -ea -cp out org.swelement.framework.util.PaintingHelper || set /a FAILED+=1
set /a TOTAL+=1

echo [ 8/62] Checking AstCloseButton...
"%JRUN%" -ea -cp out org.swelement.ui.AstCloseButton || set /a FAILED+=1
set /a TOTAL+=1

echo [ 9/62] Checking AstTag...
"%JRUN%" -ea -cp out org.swelement.ui.AstTag || set /a FAILED+=1
set /a TOTAL+=1

echo [10/62] Checking AstAlert...
"%JRUN%" -ea -cp out org.swelement.ui.AstAlert || set /a FAILED+=1
set /a TOTAL+=1

echo [11/62] Checking AstSwitch...
"%JRUN%" -ea -cp out org.swelement.ui.AstSwitch || set /a FAILED+=1
set /a TOTAL+=1

echo [12/62] Checking AstRadio...
"%JRUN%" -ea -cp out org.swelement.ui.AstRadio || set /a FAILED+=1
set /a TOTAL+=1

echo [13/62] Checking AstCheckbox...
"%JRUN%" -ea -cp out org.swelement.ui.AstCheckbox || set /a FAILED+=1
set /a TOTAL+=1

echo [14/62] Checking AstButton...
"%JRUN%" -ea -cp out org.swelement.ui.AstButton || set /a FAILED+=1
set /a TOTAL+=1

echo [15/62] Checking AstTextArea...
"%JRUN%" -ea -cp out org.swelement.ui.AstTextArea || set /a FAILED+=1
set /a TOTAL+=1

echo [16/62] Checking AstInput...
"%JRUN%" -ea -cp out org.swelement.ui.AstInput || set /a FAILED+=1
set /a TOTAL+=1

echo [17/62] Checking AstInputNumber...
"%JRUN%" -ea -cp out org.swelement.ui.AstInputNumber || set /a FAILED+=1
set /a TOTAL+=1

echo [18/62] Checking AstSelect...
"%JRUN%" -ea -cp out org.swelement.ui.AstSelect || set /a FAILED+=1
set /a TOTAL+=1

echo [19/62] Checking AstDatePicker...
"%JRUN%" -ea -cp out org.swelement.ui.AstDatePicker || set /a FAILED+=1
set /a TOTAL+=1

echo [20/62] Checking AstSteps...
"%JRUN%" -ea -cp out org.swelement.ui.AstSteps || set /a FAILED+=1
set /a TOTAL+=1

echo [21/62] Checking AstBreadcrumb...
"%JRUN%" -ea -cp out org.swelement.ui.AstBreadcrumb || set /a FAILED+=1
set /a TOTAL+=1

echo [22/62] Checking AstTabs...
"%JRUN%" -ea -cp out org.swelement.ui.AstTabs || set /a FAILED+=1
set /a TOTAL+=1

echo [23/62] Checking AstPagination...
"%JRUN%" -ea -cp out org.swelement.ui.AstPagination || set /a FAILED+=1
set /a TOTAL+=1

echo [24/62] Checking AstCollapse...
"%JRUN%" -ea -cp out org.swelement.ui.AstCollapse || set /a FAILED+=1
set /a TOTAL+=1

echo [25/62] Checking AstLoading...
"%JRUN%" -ea -cp out org.swelement.ui.AstLoading || set /a FAILED+=1
set /a TOTAL+=1

echo [26/62] Checking AstTooltip...
"%JRUN%" -ea -cp out org.swelement.ui.AstTooltip || set /a FAILED+=1
set /a TOTAL+=1

echo [27/62] Checking AstPopover...
"%JRUN%" -ea -cp out org.swelement.ui.AstPopover || set /a FAILED+=1
set /a TOTAL+=1

echo [28/62] Checking AstMessage...
"%JRUN%" -ea -cp out org.swelement.ui.AstMessage || set /a FAILED+=1
set /a TOTAL+=1

echo [29/62] Checking AstDialog...
"%JRUN%" -ea -cp out org.swelement.ui.AstDialog || set /a FAILED+=1
set /a TOTAL+=1

echo [30/62] Checking AstIcon...
"%JRUN%" -ea -cp out org.swelement.ui.AstIcon || set /a FAILED+=1
set /a TOTAL+=1

echo [31/62] Checking AstAvatar...
"%JRUN%" -ea -cp out org.swelement.ui.AstAvatar || set /a FAILED+=1
set /a TOTAL+=1

echo [32/62] Checking AstBadge...
"%JRUN%" -ea -cp out org.swelement.ui.AstBadge || set /a FAILED+=1
set /a TOTAL+=1

echo [33/62] Checking AstProgress...
"%JRUN%" -ea -cp out org.swelement.ui.AstProgress || set /a FAILED+=1
set /a TOTAL+=1

echo [34/62] Checking AstDivider...
"%JRUN%" -ea -cp out org.swelement.ui.AstDivider || set /a FAILED+=1
set /a TOTAL+=1

echo [35/62] Checking AstTimeline...
"%JRUN%" -ea -cp out org.swelement.ui.AstTimeline || set /a FAILED+=1
set /a TOTAL+=1

echo [36/62] Checking AstCard...
"%JRUN%" -ea -cp out org.swelement.ui.AstCard || set /a FAILED+=1
set /a TOTAL+=1

echo [37/62] Checking AstCalendar...
"%JRUN%" -ea -cp out org.swelement.ui.AstCalendar || set /a FAILED+=1
set /a TOTAL+=1

echo [38/62] Checking AstCarousel...
"%JRUN%" -ea -cp out org.swelement.ui.AstCarousel || set /a FAILED+=1
set /a TOTAL+=1

echo [39/62] Checking AstTree...
"%JRUN%" -ea -cp out org.swelement.ui.AstTree || set /a FAILED+=1
set /a TOTAL+=1

echo [40/62] Checking AstTable...
"%JRUN%" -ea -cp out org.swelement.ui.AstTable || set /a FAILED+=1
set /a TOTAL+=1

echo [41/62] Checking AstContainer...
"%JRUN%" -ea -cp out org.swelement.ui.AstContainer || set /a FAILED+=1
set /a TOTAL+=1

echo [42/62] Checking AstSlider...
"%JRUN%" -ea -cp out org.swelement.ui.AstSlider || set /a FAILED+=1
set /a TOTAL+=1

echo [43/62] Checking AstRate...
"%JRUN%" -ea -cp out org.swelement.ui.AstRate || set /a FAILED+=1
set /a TOTAL+=1

echo [44/62] Checking AstMenu...
"%JRUN%" -ea -cp out org.swelement.ui.AstMenu || set /a FAILED+=1
set /a TOTAL+=1

echo [45/62] Checking AstForm...
"%JRUN%" -ea -cp out org.swelement.ui.AstForm || set /a FAILED+=1
set /a TOTAL+=1

echo [46/62] Checking AstDropdown...
"%JRUN%" -ea -cp out org.swelement.ui.AstDropdown || set /a FAILED+=1
set /a TOTAL+=1

echo [47/62] Checking AstTimePicker...
"%JRUN%" -ea -cp out org.swelement.ui.AstTimePicker || set /a FAILED+=1
set /a TOTAL+=1

echo [48/62] Checking AstCascader...
"%JRUN%" -ea -cp out org.swelement.ui.AstCascader || set /a FAILED+=1
set /a TOTAL+=1

echo [49/62] Checking AstTransfer...
"%JRUN%" -ea -cp out org.swelement.ui.AstTransfer || set /a FAILED+=1
set /a TOTAL+=1

echo [50/62] Checking AstMessageBox...
"%JRUN%" -ea -cp out org.swelement.ui.AstMessageBox || set /a FAILED+=1
set /a TOTAL+=1

echo [51/62] Checking AstDrawer...
"%JRUN%" -ea -cp out org.swelement.ui.AstDrawer || set /a FAILED+=1
set /a TOTAL+=1

echo [52/62] Checking AstEmpty...
"%JRUN%" -ea -cp out org.swelement.ui.AstEmpty || set /a FAILED+=1
set /a TOTAL+=1

echo [53/62] Checking AstNotification...
"%JRUN%" -ea -cp out org.swelement.ui.AstNotification || set /a FAILED+=1
set /a TOTAL+=1

echo [54/62] Checking AstTabsDemo...
"%JRUN%" -ea -cp out org.swelement.demo.AstTabsDemo --selfcheck || set /a FAILED+=1
set /a TOTAL+=1

echo [55/62] Checking AstMenuDemo...
"%JRUN%" -ea -cp out org.swelement.demo.AstMenuDemo --selfcheck || set /a FAILED+=1
set /a TOTAL+=1

echo [56/62] Checking AstStepsDemo...
"%JRUN%" -ea -cp out org.swelement.demo.AstStepsDemo --selfcheck || set /a FAILED+=1
set /a TOTAL+=1

echo [57/62] Checking AstTimelineDemo...
"%JRUN%" -ea -cp out org.swelement.demo.AstTimelineDemo --selfcheck || set /a FAILED+=1
set /a TOTAL+=1

echo [58/62] Checking AstRadioDemo...
"%JRUN%" -ea -cp out org.swelement.demo.AstRadioDemo --selfcheck || set /a FAILED+=1
set /a TOTAL+=1

echo [59/62] Checking AstCheckboxDemo...
"%JRUN%" -ea -cp out org.swelement.demo.AstCheckboxDemo --selfcheck || set /a FAILED+=1
set /a TOTAL+=1

echo [60/62] Checking AstBreadcrumbDemo...
"%JRUN%" -ea -cp out org.swelement.demo.AstBreadcrumbDemo --selfcheck || set /a FAILED+=1
set /a TOTAL+=1

echo [61/62] Checking AstCollapseDemo...
"%JRUN%" -ea -cp out org.swelement.demo.AstCollapseDemo --selfcheck || set /a FAILED+=1
set /a TOTAL+=1

echo [62/62] Checking docs consistency (snippets / links / commands)...
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
