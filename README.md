# PWR//MON

赛博朋克风格的 Android 电池 / 充电监控 App。纯 Java + Canvas 自绘,单个全屏界面,无第三方依赖,不需要 Gradle。

在 Pixel 10a(Android 17)上开发和使用。

## 显示内容

每秒采样一次:

- **电量环**:百分比、充电状态;内圈旋转方向和速度随充放电变化
- **POWER FLOW**:进出电池的净功率(电池电压 × 电池电流),以及占协商输入上限的比例
- **数据卡片**:电压、电流(瞬时 / 平均)、温度、剩余时间、当前电量 mAh、循环次数与健康状态、电源类型与协商档位、本次峰值功率
- **功率曲线**:最近 120 秒

主色随状态变化:充电青色、放电品红、低电量红色、充满绿色。App 在前台时保持屏幕常亮。

## 数据来源与限制

只用公开的系统接口(`BatteryManager` 和 `ACTION_BATTERY_CHANGED` 广播),不需要任何权限,也不需要 root。因此:

- 功率是**电池端**的净功率,已经扣掉了系统自身耗电和转换损耗,会明显低于充电头的实际输出
- 读不到 USB 输入端的实测电压 / 电流,只能显示协商到的最高电压和电流
- 满电容量是用「当前 mAh ÷ 百分比」估算的,不是电量计里的值

## 构建与安装

需要 JDK 17 或更高版本,以及 Android SDK 的 `build-tools;36.0.0` 和 `platforms;android-36`。SDK 路径取自 `ANDROID_SDK_ROOT`,默认 `~/Library/Android/sdk`。

```sh
./build.sh            # 生成 build/pwrmon.apk
./build.sh install    # 构建后通过 adb 安装并启动
```

构建脚本直接调用 `aapt2`、`javac`、`d8`、`zipalign`、`apksigner`,用 `~/.android/debug.keystore` 签名(不存在时自动生成)。

最低支持 Android 11(API 30)。

## 目录

```
AndroidManifest.xml
build.sh
res/                              图标和主题
src/com/xcl/pwrmon/
  MainActivity.java               窗口设置、启停采样
  MonitorView.java                采样与全部绘制
```
