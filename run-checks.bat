@echo off
setlocal enabledelayedexpansion
rem ---- Use Java 8 if available (target JDK 1.8) ----
set "JAVA8=C:\Program Files\Java\jdk1.8.0_311"
set "JRUN=java"
if exist "%JAVA8%\bin\java.exe" (
  set "JRUN=%JAVA8%\bin\java.exe"
) else (
  echo WARN: JDK 1.8 not found, falling back to java on PATH
)
"%JRUN%" -version >nul 2>nul || (echo ERROR: java not found & exit /b 1)

echo ========================================
echo   Swing Element UI - Self-Check Suite
echo ========================================
echo.

set FAILED=0
set TOTAL=0

echo [ 1/51] Checking Easing...
"%JRUN%" -ea -cp out org.swelement.core.Easing || set /a FAILED+=1
set /a TOTAL+=1

echo [ 2/51] Checking ElementTheme...
"%JRUN%" -ea -cp out org.swelement.core.ElementTheme || set /a FAILED+=1
set /a TOTAL+=1

echo [ 3/51] Checking ThemeManager...
"%JRUN%" -ea -cp out org.swelement.core.theme.ThemeManager || set /a FAILED+=1
set /a TOTAL+=1

echo [ 4/51] Checking ElementLightTheme...
"%JRUN%" -ea -cp out org.swelement.core.theme.ElementLightTheme || set /a FAILED+=1
set /a TOTAL+=1

echo [ 5/51] Checking AnimationManager...
"%JRUN%" -ea -cp out org.swelement.core.AnimationManager || set /a FAILED+=1
set /a TOTAL+=1

echo [ 6/51] Checking SelfCheckBase...
"%JRUN%" -ea -cp out org.swelement.core.SelfCheckBase || set /a FAILED+=1
set /a TOTAL+=1

echo [ 7/51] Checking PaintingHelper...
"%JRUN%" -ea -cp out org.swelement.framework.util.PaintingHelper || set /a FAILED+=1
set /a TOTAL+=1

echo [ 8/51] Checking AstCloseButton...
"%JRUN%" -ea -cp out org.swelement.ui.AstCloseButton || set /a FAILED+=1
set /a TOTAL+=1

echo [ 9/51] Checking AstTag...
"%JRUN%" -ea -cp out org.swelement.ui.AstTag || set /a FAILED+=1
set /a TOTAL+=1

echo [10/51] Checking AstAlert...
"%JRUN%" -ea -cp out org.swelement.ui.AstAlert || set /a FAILED+=1
set /a TOTAL+=1

echo [11/51] Checking AstSwitch...
"%JRUN%" -ea -cp out org.swelement.ui.AstSwitch || set /a FAILED+=1
set /a TOTAL+=1

echo [12/51] Checking AstRadio...
"%JRUN%" -ea -cp out org.swelement.ui.AstRadio || set /a FAILED+=1
set /a TOTAL+=1

echo [13/51] Checking AstCheckbox...
"%JRUN%" -ea -cp out org.swelement.ui.AstCheckbox || set /a FAILED+=1
set /a TOTAL+=1

echo [14/51] Checking AstButton...
"%JRUN%" -ea -cp out org.swelement.ui.AstButton || set /a FAILED+=1
set /a TOTAL+=1

echo [15/51] Checking AstTextArea...
"%JRUN%" -ea -cp out org.swelement.ui.AstTextArea || set /a FAILED+=1
set /a TOTAL+=1

echo [16/51] Checking AstInput...
"%JRUN%" -ea -cp out org.swelement.ui.AstInput || set /a FAILED+=1
set /a TOTAL+=1

echo [17/51] Checking AstInputNumber...
"%JRUN%" -ea -cp out org.swelement.ui.AstInputNumber || set /a FAILED+=1
set /a TOTAL+=1

echo [18/51] Checking AstSelect...
"%JRUN%" -ea -cp out org.swelement.ui.AstSelect || set /a FAILED+=1
set /a TOTAL+=1

echo [19/51] Checking AstDatePicker...
"%JRUN%" -ea -cp out org.swelement.ui.AstDatePicker || set /a FAILED+=1
set /a TOTAL+=1

echo [20/51] Checking AstSteps...
"%JRUN%" -ea -cp out org.swelement.ui.AstSteps || set /a FAILED+=1
set /a TOTAL+=1

echo [21/51] Checking AstBreadcrumb...
"%JRUN%" -ea -cp out org.swelement.ui.AstBreadcrumb || set /a FAILED+=1
set /a TOTAL+=1

echo [22/51] Checking AstTabs...
"%JRUN%" -ea -cp out org.swelement.ui.AstTabs || set /a FAILED+=1
set /a TOTAL+=1

echo [23/51] Checking AstPagination...
"%JRUN%" -ea -cp out org.swelement.ui.AstPagination || set /a FAILED+=1
set /a TOTAL+=1

echo [24/51] Checking AstCollapse...
"%JRUN%" -ea -cp out org.swelement.ui.AstCollapse || set /a FAILED+=1
set /a TOTAL+=1

echo [25/51] Checking AstLoading...
"%JRUN%" -ea -cp out org.swelement.ui.AstLoading || set /a FAILED+=1
set /a TOTAL+=1

echo [26/51] Checking AstTooltip...
"%JRUN%" -ea -cp out org.swelement.ui.AstTooltip || set /a FAILED+=1
set /a TOTAL+=1

echo [27/51] Checking AstPopover...
"%JRUN%" -ea -cp out org.swelement.ui.AstPopover || set /a FAILED+=1
set /a TOTAL+=1

echo [28/51] Checking AstMessage...
"%JRUN%" -ea -cp out org.swelement.ui.AstMessage || set /a FAILED+=1
set /a TOTAL+=1

echo [29/51] Checking AstDialog...
"%JRUN%" -ea -cp out org.swelement.ui.AstDialog || set /a FAILED+=1
set /a TOTAL+=1

echo [30/51] Checking AstIcon...
"%JRUN%" -ea -cp out org.swelement.ui.AstIcon || set /a FAILED+=1
set /a TOTAL+=1

echo [31/51] Checking AstAvatar...
"%JRUN%" -ea -cp out org.swelement.ui.AstAvatar || set /a FAILED+=1
set /a TOTAL+=1

echo [32/51] Checking AstBadge...
"%JRUN%" -ea -cp out org.swelement.ui.AstBadge || set /a FAILED+=1
set /a TOTAL+=1

echo [33/51] Checking AstProgress...
"%JRUN%" -ea -cp out org.swelement.ui.AstProgress || set /a FAILED+=1
set /a TOTAL+=1

echo [34/51] Checking AstDivider...
"%JRUN%" -ea -cp out org.swelement.ui.AstDivider || set /a FAILED+=1
set /a TOTAL+=1

echo [35/51] Checking AstTimeline...
"%JRUN%" -ea -cp out org.swelement.ui.AstTimeline || set /a FAILED+=1
set /a TOTAL+=1

echo [36/51] Checking AstCard...
"%JRUN%" -ea -cp out org.swelement.ui.AstCard || set /a FAILED+=1
set /a TOTAL+=1

echo [37/51] Checking AstCalendar...
"%JRUN%" -ea -cp out org.swelement.ui.AstCalendar || set /a FAILED+=1
set /a TOTAL+=1

echo [38/51] Checking AstCarousel...
"%JRUN%" -ea -cp out org.swelement.ui.AstCarousel || set /a FAILED+=1
set /a TOTAL+=1

echo [39/51] Checking AstTree...
"%JRUN%" -ea -cp out org.swelement.ui.AstTree || set /a FAILED+=1
set /a TOTAL+=1

echo [40/51] Checking AstTable...
"%JRUN%" -ea -cp out org.swelement.ui.AstTable || set /a FAILED+=1
set /a TOTAL+=1

echo [41/51] Checking AstContainer...
"%JRUN%" -ea -cp out org.swelement.ui.AstContainer || set /a FAILED+=1
set /a TOTAL+=1

echo [42/51] Checking AstSlider...
"%JRUN%" -ea -cp out org.swelement.ui.AstSlider || set /a FAILED+=1
set /a TOTAL+=1

echo [43/51] Checking AstRate...
"%JRUN%" -ea -cp out org.swelement.ui.AstRate || set /a FAILED+=1
set /a TOTAL+=1

echo [44/51] Checking AstMenu...
"%JRUN%" -ea -cp out org.swelement.ui.AstMenu || set /a FAILED+=1
set /a TOTAL+=1

echo [45/51] Checking AstForm...
"%JRUN%" -ea -cp out org.swelement.ui.AstForm || set /a FAILED+=1
set /a TOTAL+=1

echo [46/51] Checking AstDropdown...
"%JRUN%" -ea -cp out org.swelement.ui.AstDropdown || set /a FAILED+=1
set /a TOTAL+=1

echo [47/51] Checking AstTimePicker...
"%JRUN%" -ea -cp out org.swelement.ui.AstTimePicker || set /a FAILED+=1
set /a TOTAL+=1

echo [48/51] Checking AstCascader...
"%JRUN%" -ea -cp out org.swelement.ui.AstCascader || set /a FAILED+=1
set /a TOTAL+=1

echo [49/51] Checking AstTransfer...
"%JRUN%" -ea -cp out org.swelement.ui.AstTransfer || set /a FAILED+=1
set /a TOTAL+=1

echo [50/51] Checking AstMessageBox...
"%JRUN%" -ea -cp out org.swelement.ui.AstMessageBox || set /a FAILED+=1
set /a TOTAL+=1

echo [51/51] Checking AstDrawer...
"%JRUN%" -ea -cp out org.swelement.ui.AstDrawer || set /a FAILED+=1
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
