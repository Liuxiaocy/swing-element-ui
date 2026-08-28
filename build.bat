@echo off
setlocal enabledelayedexpansion
set "JAVAC=javac"
if exist "C:\Program Files\Java\jdk1.8.0_311\bin\javac.exe" set "JAVAC=C:\Program Files\Java\jdk1.8.0_311\bin\javac.exe"
"%JAVAC%" -version >nul 2>nul || (echo ERROR: javac not found & exit /b 1)
if not exist out mkdir out

SET SOURCES=^
src\org\swelement\core\ElementTheme.java ^
src\org\swelement\core\Easing.java ^
src\org\swelement\core\Animator.java ^
src\org\swelement\core\AnimatedPopup.java ^
src\org\swelement\core\GlassPane.java ^
src\org\swelement\core\PopupPositioner.java ^
src\org\swelement\ui\Alert.java ^
src\org\swelement\ui\Badge.java ^
src\org\swelement\ui\Button.java ^
src\org\swelement\ui\Checkbox.java ^
src\org\swelement\ui\CloseButton.java ^
src\org\swelement\ui\Input.java ^
src\org\swelement\ui\TextArea.java ^
src\org\swelement\ui\Menu.java ^
src\org\swelement\ui\Pagination.java ^
src\org\swelement\ui\Progress.java ^
src\org\swelement\ui\Radio.java ^
src\org\swelement\ui\Select.java ^
src\org\swelement\ui\Slider.java ^
src\org\swelement\ui\Switch.java ^
src\org\swelement\ui\Tabs.java ^
src\org\swelement\ui\Tag.java ^
src\org\swelement\demo\AlertDemo.java ^
src\org\swelement\demo\BadgeDemo.java ^
src\org\swelement\demo\ButtonDemo.java ^
src\org\swelement\demo\CheckboxDemo.java ^
src\org\swelement\demo\InputDemo.java ^
src\org\swelement\demo\MenuDemo.java ^
src\org\swelement\demo\PaginationDemo.java ^
src\org\swelement\demo\ProgressDemo.java ^
src\org\swelement\demo\RadioDemo.java ^
src\org\swelement\demo\SelectDemo.java ^
src\org\swelement\demo\SliderDemo.java ^
src\org\swelement\demo\SwitchDemo.java ^
src\org\swelement\demo\TabsDemo.java ^
src\org\swelement\demo\TagDemo.java ^
src\org\swelement\ui\AstContainer.java ^
src\org\swelement\demo\AstContainerDemo.java ^
src\org\swelement\ui\AstAvatar.java ^
src\org\swelement\demo\AstAvatarDemo.java ^
src\org\swelement\ui\AstCard.java ^
src\org\swelement\demo\AstCardDemo.java ^
src\org\swelement\ui\AstLoading.java ^
src\org\swelement\demo\AstLoadingDemo.java ^
src\org\swelement\ui\AstTooltip.java ^
src\org\swelement\ui\AstDropdown.java ^
src\org\swelement\ui\AstDialog.java ^
src\org\swelement\ui\AstMessageBox.java ^
src\org\swelement\ui\AstMessage.java ^
src\org\swelement\demo\AstPopupDemo.java ^
src\org\swelement\ui\AstCascader.java ^
src\org\swelement\ui\AstDatePicker.java ^
src\org\swelement\ui\AstForm.java ^
src\org\swelement\ui\AstTree.java ^
src\org\swelement\ui\AstTable.java ^
src\org\swelement\ui\AstTableColumn.java ^
src\org\swelement\ui\AstTableModel.java ^
src\org\swelement\demo\AstTableDemo.java ^
src\org\swelement\demo\AstAdvancedDemo.java ^
src\org\swelement\demo\AstP2P3Demo.java ^
src\org\swelement\demo\AstFormDemo.java ^
src\org\swelement\ui\AstDivider.java ^
src\org\swelement\ui\AstIcon.java ^
src\org\swelement\ui\AstRate.java ^
src\org\swelement\ui\AstBreadcrumb.java ^
src\org\swelement\ui\AstSteps.java ^
src\org\swelement\ui\AstCollapse.java ^
src\org\swelement\ui\AstInputNumber.java ^
src\org\swelement\ui\AstPopover.java ^
src\org\swelement\ui\AstDrawer.java ^
src\org\swelement\ui\AstTimePicker.java ^
src\org\swelement\ui\AstTransfer.java ^
src\org\swelement\ui\AstTimeline.java ^
src\org\swelement\ui\AstCalendar.java ^
src\org\swelement\ui\AstCarousel.java

"%JAVAC%" -encoding UTF-8 --release 8 -d out %SOURCES%
if errorlevel 1 (
  echo --release 8 not supported, retrying with -source/-target 8
  "%JAVAC%" -encoding UTF-8 -source 8 -target 8 -d out %SOURCES%
)
if errorlevel 1 (echo BUILD FAILED & exit /b 1)
echo BUILD OK

echo --- CloseButton self-check ---
java -ea -cp out org.swelement.ui.AstCloseButton
if %ERRORLEVEL% NEQ 0 ( echo CloseButton self-check FAILED & exit /b 1 )

echo --- Tag self-check ---
java -ea -cp out org.swelement.ui.AstTag
if %ERRORLEVEL% NEQ 0 ( echo Tag self-check FAILED & exit /b 1 )

echo --- Alert self-check ---
java -ea -cp out org.swelement.ui.AstAlert
if %ERRORLEVEL% NEQ 0 ( echo Alert self-check FAILED & exit /b 1 )

echo --- Input self-check ---
java -ea -cp out org.swelement.ui.AstInput
if %ERRORLEVEL% NEQ 0 ( echo Input self-check FAILED & exit /b 1 )

echo --- TextArea self-check ---
java -ea -cp out org.swelement.ui.AstTextArea
if %ERRORLEVEL% NEQ 0 ( echo TextArea self-check FAILED & exit /b 1 )

echo --- AstContainer self-check ---
java -ea -cp out org.swelement.ui.AstContainer
if %ERRORLEVEL% NEQ 0 ( echo AstContainer self-check FAILED & exit /b 1 )

echo --- AstAvatar self-check ---
java -ea -cp out org.swelement.ui.AstAvatar
if %ERRORLEVEL% NEQ 0 ( echo AstAvatar self-check FAILED & exit /b 1 )

echo --- AstCard self-check ---
java -ea -cp out org.swelement.ui.AstCard
if %ERRORLEVEL% NEQ 0 ( echo AstCard self-check FAILED & exit /b 1 )

echo --- AstLoading self-check ---
java -ea -cp out org.swelement.ui.AstLoading
if %ERRORLEVEL% NEQ 0 ( echo AstLoading self-check FAILED & exit /b 1 )

echo --- AstTooltip self-check ---
java -ea -cp out org.swelement.ui.AstTooltip
if %ERRORLEVEL% NEQ 0 ( echo AstTooltip self-check FAILED & exit /b 1 )

echo --- AstDropdown self-check ---
java -ea -cp out org.swelement.ui.AstDropdown
if %ERRORLEVEL% NEQ 0 ( echo AstDropdown self-check FAILED & exit /b 1 )

echo --- AstDialog self-check ---
java -ea -cp out org.swelement.ui.AstDialog
if %ERRORLEVEL% NEQ 0 ( echo AstDialog self-check FAILED & exit /b 1 )

echo --- AstMessageBox self-check ---
java -ea -cp out org.swelement.ui.AstMessageBox
if %ERRORLEVEL% NEQ 0 ( echo AstMessageBox self-check FAILED & exit /b 1 )

echo --- AstMessage self-check ---
java -ea -cp out org.swelement.ui.AstMessage
if %ERRORLEVEL% NEQ 0 ( echo AstMessage self-check FAILED & exit /b 1 )

echo --- AstCascader self-check ---
java -ea -cp out org.swelement.ui.AstCascader
if %ERRORLEVEL% NEQ 0 ( echo AstCascader self-check FAILED & exit /b 1 )

echo --- AstDatePicker self-check ---
java -ea -cp out org.swelement.ui.AstDatePicker
if %ERRORLEVEL% NEQ 0 ( echo AstDatePicker self-check FAILED & exit /b 1 )

echo --- AstForm self-check ---
java -ea -cp out org.swelement.ui.AstForm
if %ERRORLEVEL% NEQ 0 ( echo AstForm self-check FAILED & exit /b 1 )

echo --- AstTree self-check ---
java -ea -cp out org.swelement.ui.AstTree
if %ERRORLEVEL% NEQ 0 ( echo AstTree self-check FAILED & exit /b 1 )

echo --- AstTable self-check ---
java -ea -cp out org.swelement.ui.AstTable
if %ERRORLEVEL% NEQ 0 ( echo AstTable self-check FAILED & exit /b 1 )

echo --- AstTableDemo self-check ---
java -ea -cp out org.swelement.demo.AstTableDemo --selfcheck
if %ERRORLEVEL% NEQ 0 ( echo AstTableDemo self-check FAILED & exit /b 1 )

echo --- AstDivider self-check ---
java -ea -cp out org.swelement.ui.AstDivider
if %ERRORLEVEL% NEQ 0 ( echo AstDivider self-check FAILED & exit /b 1 )

echo --- AstIcon self-check ---
java -ea -cp out org.swelement.ui.AstIcon
if %ERRORLEVEL% NEQ 0 ( echo AstIcon self-check FAILED & exit /b 1 )

echo --- AstRate self-check ---
java -ea -cp out org.swelement.ui.AstRate
if %ERRORLEVEL% NEQ 0 ( echo AstRate self-check FAILED & exit /b 1 )

echo --- AstBreadcrumb self-check ---
java -ea -cp out org.swelement.ui.AstBreadcrumb
if %ERRORLEVEL% NEQ 0 ( echo AstBreadcrumb self-check FAILED & exit /b 1 )

echo --- AstSteps self-check ---
java -ea -cp out org.swelement.ui.AstSteps
if %ERRORLEVEL% NEQ 0 ( echo AstSteps self-check FAILED & exit /b 1 )

echo --- AstCollapse self-check ---
java -ea -cp out org.swelement.ui.AstCollapse
if %ERRORLEVEL% NEQ 0 ( echo AstCollapse self-check FAILED & exit /b 1 )

echo --- AstInputNumber self-check ---
java -ea -cp out org.swelement.ui.AstInputNumber
if %ERRORLEVEL% NEQ 0 ( echo AstInputNumber self-check FAILED & exit /b 1 )

echo --- AstPopover self-check ---
java -ea -cp out org.swelement.ui.AstPopover
if %ERRORLEVEL% NEQ 0 ( echo AstPopover self-check FAILED & exit /b 1 )

echo --- AstDrawer self-check ---
java -ea -cp out org.swelement.ui.AstDrawer
if %ERRORLEVEL% NEQ 0 ( echo AstDrawer self-check FAILED & exit /b 1 )

echo --- AstTimePicker self-check ---
java -ea -cp out org.swelement.ui.AstTimePicker
if %ERRORLEVEL% NEQ 0 ( echo AstTimePicker self-check FAILED & exit /b 1 )

echo --- AstTransfer self-check ---
java -ea -cp out org.swelement.ui.AstTransfer
if %ERRORLEVEL% NEQ 0 ( echo AstTransfer self-check FAILED & exit /b 1 )

echo --- AstTimeline self-check ---
java -ea -cp out org.swelement.ui.AstTimeline
if %ERRORLEVEL% NEQ 0 ( echo AstTimeline self-check FAILED & exit /b 1 )

echo --- AstCalendar self-check ---
java -ea -cp out org.swelement.ui.AstCalendar
if %ERRORLEVEL% NEQ 0 ( echo AstCalendar self-check FAILED & exit /b 1 )

echo --- AstCarousel self-check ---
java -ea -cp out org.swelement.ui.AstCarousel
if %ERRORLEVEL% NEQ 0 ( echo AstCarousel self-check FAILED & exit /b 1 )
