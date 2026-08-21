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
src\org\swelement\ui\Input.java ^
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
src\org\swelement\ui\AstDatePicker.java

"%JAVAC%" -encoding UTF-8 --release 8 -d out %SOURCES%
if errorlevel 1 (
  echo --release 8 not supported, retrying with -source/-target 8
  "%JAVAC%" -encoding UTF-8 -source 8 -target 8 -d out %SOURCES%
)
if errorlevel 1 (echo BUILD FAILED & exit /b 1)
echo BUILD OK

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
