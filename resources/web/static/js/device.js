
let ip = "127.0.0.1"
let port = 6655
let lastBtn='group'
let groupDir=null

/** nav-height:24px */
const nav_height = 24;
/** footer-height: 42px */
const footer_height = 42;

let deviceSize = {
    w: 1080,
    h: 1920
}

var group_dir = document.getElementById('group-dir');
// 创建输入框
var group_dir_input = document.createElement('input');
// 设置输入框的属性
var device_code = document.getElementById('device-code');
// 创建输入框
var device_code_input = document.createElement('input');
// 设置输入框的属性
var cmd_cli=document.getElementById('cli-cmd');
var cmd_cli_input=document.createElement('input');
var server_port=document.getElementById("server-port");
var server_port_input=document.createElement('input');
group_dir.appendChild(group_dir_input);
device_code.appendChild(device_code_input);
cmd_cli.appendChild(cmd_cli_input);
server_port.appendChild(server_port_input);
/**
 * 描述deivce信息的类
 */
class DeviceInfo {
    constructor() {
        // 设备的物理大小
        this.physicsSize = {
            w: 0,
            h: 0
        }
        this.serialNumber = ""
    }
}

/**
 * 对该窗口的操作类
 */
class DeviceWindow {
    constructor(win, deviceInfo, defaultDisplaySize={w:0, h:0}) {
        this.win = win // 操作的窗口
        this.deviceInfo = deviceInfo
        this.scale = 0.4 // minicap缩放比例
        this.rotate = false // 屏幕是否旋转，默认=false=竖屏
        this.keyMap = false // 是否键盘映射
        this.displaySize = defaultDisplaySize
    }

    resize(setCenter = true) {

        if (this.rotate) {
            [this.displaySize.w, this.displaySize.h] = [this.displaySize.h, this.displaySize.w]
        }

        // vue
        title.displaySize = this.displaySize

        let w = this.displaySize.w
        let h = this.displaySize.h + nav_height + footer_height
        this.win.css('width', w + "px")
        this.win.css('height', h + "px")
    }
}

/**
 * 网络操作
 */
class NetWork {
    constructor(ip, port) {
        this.ip = ip
        this.port = port
    }

    connect(config) {
        let webSocket = new WebSocket("ws://" + ip + ":" + port)
        webSocket.onopen = function() {
            config.onopen()
        }
        webSocket.onclose = function() {
            config.onclose()
        }
        webSocket.onmessage = function(data) { 
            config.onmessage(data)
        }
        this.webSocket = webSocket
    }

    request(name, argobj) {
        let ss = name + "://" + (argobj ? JSON.stringify(argobj) : "{}");
        this.webSocket.send(ss);
    }

    send(str) {
        this.webSocket.send(str)
    }
}

/**
 * Device 的 Vue 组件
 */

let deviceInfo = new DeviceInfo()
let deviceWindow = null
let net = null

let title = new Vue({
    el: '#title',
    data: {
        displaySize: {w: 1080, h: 1920},
        outputScale: 0.3
    },
    computed: {
        title: function() {
            return this.displaySize.w + "x" + this.displaySize.h + "  |  " + parseInt(deviceInfo.physicsSize.w*this.outputScale)+ "x" + parseInt(deviceInfo.physicsSize.h*this.outputScale);
        }
    }
})


window.onload = function() {

    // 通过url参数初始化
    let urlParams = initWithUrlParams();

    deviceInfo.serialNumber = urlParams.sn
    
    deviceInfo.physicsSize.w = urlParams.w
    deviceInfo.physicsSize.h = urlParams.h
    
    // 滑动条初始化
    var displayScaleSlider = $("#display-scale-slider").slider({
        max: 100,
        min: 10,
        step: 5,
        value: 20,
        change: onDisplayScaleChange
    })

    var scaleSlider = $('#scale-slider').slider({
        max: 100,
        min: 5,
        step: 5,
        value: 30,
        change: onScaleChange
    })

    $('#rotateCheckBox').on('click', function() {
        deviceWindow.rotate = $('#rotateCheckBox').prop('checked')
        net.request("M_START", {type: "cap", config: {rotate: deviceWindow.rotate ? 90 : 0, scale: deviceWindow.scale}})
        // 隐藏设置窗口
        $('#myModal').modal('hide')
        // 显示等待capservice窗口
        $('#resetScaleModal').modal('show')

        onDisplayScaleChange()
    })

    $('#keyEventCheckBox').on('click', function() {
        deviceWindow.keyMap = $('#keyEventCheckBox').prop('checked')
    })


    function onDisplayScaleChange() {
        let scale = displayScaleSlider.slider("value") / 100.0;
        deviceWindow.displaySize.w = parseInt(deviceInfo.physicsSize.w * scale)
        deviceWindow.displaySize.h = parseInt(deviceInfo.physicsSize.h * scale)
        deviceWindow.resize(false)

        canvas.width = deviceWindow.displaySize.w;
        canvas.height = deviceWindow.displaySize.h;
        g.drawImage(canvas.img, 0, 0, canvas.width, canvas.height);
    }

    function onScaleChange() {
        let scale = scaleSlider.slider("value") / 100.0

        deviceWindow.scale = scale
        // vue
        title.outputScale = scale
        net.request("M_START", {type: "cap", config: {rotate: deviceWindow.rotate ? 90 : 0, scale: deviceWindow.scale}})
        // 隐藏设置窗口
        $('#myModal').modal('hide')
        // 显示等待capservice窗口
        $('#resetScaleModal').modal('show')
    }

    // 初始化窗口
    let scale = displayScaleSlider.slider("value") / 100.0;
    deviceWindow = new DeviceWindow($('#content'), deviceInfo, {
        w: deviceInfo.physicsSize.w * scale, 
        h: deviceInfo.physicsSize.h * scale
    })

    deviceWindow.resize()

    // vue
    title.outputScale = scale

    // 连接服务器
    net = new NetWork(ip, port)
    net.connect({
        onopen() {
            net.request("M_WAIT", {sn: deviceInfo.serialNumber})
        },
        onclose() {
            deviceWindow.win.close()
        },
        onmessage(msg) {
            let data = msg.data
            if (typeof(data) == 'string') {
                this.ontext(data)
            } else {
                this.onbinary(data)
            }
        },
        ontext(text) {
            let sp = text.indexOf('://')
            if (sp == -1) {
                console.log("无效的协议")
                this.onclose()
            }

            let head = text.substr(0, sp)
            let body = text.substring(sp + 3)

            let func = this[head]
            func.call(this, body)
        },
        onbinary(data) {
            let self = this
            let fr = new FileReader()
            fr.readAsArrayBuffer(data.slice(0, 2))
            fr.onload = function() {
                let headType = new Int16Array(fr.result)[0]
                switch (headType) {
                    case 0x0011:
                        self.SM_JPG(data.slice(6))
                    break;
                }
            }
        },
        SM_OPENED(body) {
            net.request("M_START", {type: "cap", config: {rotate: deviceWindow.rotate ? 90 : 0, scale: deviceWindow.scale}})
            net.request("M_START", {type: "event"})
        },
        SM_SERVICE_STATE(body) {
            console.log("SM_SERVICE_STATE" + body)
            let obj = JSON.parse(body)
            console.warn(obj.type + ":" + obj.stat)
            if (obj.type == 'cap' && obj.stat == 'open') {
                // 隐藏等待capservice的窗口
                $('#resetScaleModal').modal('hide')
                this.M_WAITTING()
            }
        },
        SM_JPG(jpgdata) {
            var blob = new Blob([jpgdata], {type: 'image/jpeg'});
            var URL = window.URL || window.webkitURL;
            var img = new Image();
            img.onload = function () {
                canvas.width = parseInt(deviceWindow.displaySize.w);
                canvas.height = parseInt(deviceWindow.displaySize.h);
                console.log(canvas.width, canvas.height)
                g.drawImage(img, 0, 0, canvas.width, canvas.height);
                img.onLoad = null;
                img = null;
                u = null;
                blob = null;
            };
            var u = URL.createObjectURL(blob);
            img.src = u;
            canvas.img = img
            
            if (deviceWindow.resized) {
                deviceWindow.resize()
            }

            this.M_WAITTING()
        },
        M_WAITTING() {
            net.request("M_WAITTING", null)
        }
    })
}

/**
 * 返回url参数组成的js对象
 */
function initWithUrlParams() {
    let ret = {}
    let ss = window.location.search.substr(1).split('&')
    for (s of ss) {
        let sp = s.split('=')
        ret[sp[0]] = sp[1]
    }
    return ret
}

let isDown = false

var canvas = document.getElementById("phone-screen");
var g = canvas.getContext('2d');


String.prototype.startWith=function(str){
    var reg=new RegExp("^"+str);
    return reg.test(this);
};

String.prototype.endWith=function(str){
    var reg=new RegExp(str+"$");
    return reg.test(this);
};
/**
 * 输入数值的检查
 */
group_dir_input.addEventListener('blur', function(event) {
    console.log("输入框的值为：" + event.target.value);
    if(!isValidPath(event.target.value)) {window.alert("非法地址");group_dir_input.value="";}
});

device_code_input.addEventListener('blur', function(event) {
    console.log("输入框的值为：" + event.target.value);
});

$("#btn-menu").on('click', function(){
    sendKeyEvent(82)
})

$("#btn-home").on('click', function(){
    sendKeyEvent(3)
})

$("#btn-back").on('click', function(){
    sendKeyEvent(4)
})
$('#set-constant').on('click',function(){
    net.request("M_START", {type: "constant", config: {}})
    window,alert("已设置常量状态");
})

$('#turnoff-mobiledata').on('click',function(){
    net.request("M_START", {type: "turnoff", config: {}})
    window,alert("已关闭数据流量");
})

$('#make-sound').on('click',function(){
        net.request("M_START", {type: "ring", config: {}})
})

$('input:radio').click(function(){
    var $radio = $(this);
    if ($radio.data('waschecked') == true){
        $radio.prop('checked', false); $radio.data('waschecked', false);
        isDefualtAdd = 0;
    } else {
        $radio.prop('checked', true); $radio.data('waschecked', true);
        isDefualtAdd = 1;
    }
});


/**
 * app安装
 */
$('#install-app').on('click',function(){
    let dir=group_dir_input.value;
    let code=device_code_input.value;
    if(isNotEmptyStr(dir)&&isNotEmptyStr(code)){
        window.alert("正准备安装，请在等待过程中不要执行其它操作.");
        var tesObj = document.getElementsByName("install-tool");
        //获取选中的值
        var isAdd;
        for(var i=0; i < tesObj.length; i++){
            if (tesObj[i].checked==true){
                isAdd=tesObj[i].value;
                alert(tesObj[i].value+'  是选中的value值');
                break;
            }
        }
        if(isAdd=="1")
        net.request("M_START", {type: "install", config: {"dir":dir,"code":code,"addTool":true}});
        else
        net.request("M_START", {type: "install", config: {"dir":dir,"code":code,"addTool":false}});
    } else window.alert("请在参数设置中填写项目安装地址与设备编号");
})
/**
 * cli命令运行
 */
$('#run-cmd').on('click',function (){
    let cmd=cmd_cli_input.value;
    let port=server_port_input.value;
  if(isNotEmptyStr(cmd)){
      net.request("M_START", {type: "callCLi", config: {"port":port,"cmd":cmd}});
  }else window.alert("请输入命令与端口值");
})


$('#reset-system').on('click',function(){
    var isContinue=window.confirm("请确认是否重启至recovery模式？");
    if (isContinue==true) {
     net.request("M_START", {type: "reset", config: {}})
    }
})




$(document).keypress(function(event) {
    if (deviceWindow && deviceWindow.keyMap) {
        let code = event.keyCode
        let keyEvent = convertAndroidKeyCode(code)
        sendKeyEvent(keyEvent)
    }
})

setLastDivTag(lastBtn)
$('#group').on('click', function() {
    console.log("Do Group")
    document.getElementById('make-task').style.display = 'block';
    document.getElementById('run-operation').style.display = 'none';
    document.getElementById('search-info').style.display = 'none';
    document.getElementById('value-settings').style.display = 'none';
    console.log("lastBtn"+getLastDivTag())
    if(isNotEmptyStr(getLastDivTag())) {a = document.getElementById(getLastDivTag());a.style.backgroundColor = '#9d9d9d'}
    // if(isNotEmptyAndEqualsStr(getLastDivTag(),'group')) { a = document.getElementById(getLastDivTag()); a.style.backgroundColor = '#FFFFFF' ;}
    b = document.getElementById('group');
    b.style.backgroundColor = '#FFFFFF';
    setLastDivTag("group");


})





$('#operation').on('click', function() {
    console.log("Make Operation")
    document.getElementById('make-task').style.display = 'none';
    document.getElementById('run-operation').style.display = 'block';
    document.getElementById('search-info').style.display = 'none';
    document.getElementById('value-settings').style.display = 'none';
    console.log("lastBtn"+getLastDivTag())
    if(isNotEmptyStr(getLastDivTag())) {a = document.getElementById(getLastDivTag());a.style.backgroundColor = '#9d9d9d'}
    // if(isNotEmptyAndEqualsStr(getLastDivTag(),"operation")) { a = document.getElementById(getLastDivTag()); a.style.backgroundColor = '#FFFFFF' };
    b = document.getElementById('operation');
    b.style.backgroundColor = '#FFFFFF';
    setLastDivTag("operation");
})

$('#farewell').on('click', function() {
    console.log("Do Group")
    document.getElementById('make-task').style.display = 'none';
    document.getElementById('run-operation').style.display = 'none';
    document.getElementById('search-info').style.display = 'block';
    document.getElementById('value-settings').style.display = 'none';
    console.log("lastBtn"+getLastDivTag())
    if(isNotEmptyStr(getLastDivTag())) {a = document.getElementById(getLastDivTag());a.style.backgroundColor = '#9d9d9d'}
    // if(isNotEmptyAndEqualsStr(getLastDivTag(),'group')) { a = document.getElementById(getLastDivTag()); a.style.backgroundColor = '#FFFFFF' ;}
    b = document.getElementById('farewell');
    b.style.backgroundColor = '#FFFFFF';
    setLastDivTag("farewell");
})

$('#settings').on('click', function() {
    document.getElementById('make-task').style.display = 'none';
    document.getElementById('run-operation').style.display = 'none';
    document.getElementById('search-info').style.display = 'none';
    document.getElementById('value-settings').style.display = 'block';
    console.log("lastBtn"+getLastDivTag())
    if(isNotEmptyStr(getLastDivTag())) {a = document.getElementById(getLastDivTag());a.style.backgroundColor = '#9d9d9d'}
    // if(isNotEmptyAndEqualsStr(getLastDivTag(),'group')) { a = document.getElementById(getLastDivTag()); a.style.backgroundColor = '#FFFFFF' ;}
    b = document.getElementById('settings');
    b.style.backgroundColor = '#FFFFFF';
    setLastDivTag("settings");
})

function setLastDivTag(s){
    this.lastBtn=s;
}

function getLastDivTag(){
    return this.lastBtn;
}

function setDeviceCode(s){
    this.code=s;
}

function getDeviceCode(){
    return this.code;
}

function setGroupDir(s){
    this.groupDir=s;
}

function getGroupDir(){
    return groupDir;
}

/**
 * 判断输入地址是否合法
 * @param dir
 * @returns {boolean}
 */
function isValidPath(dir){
    var path = new RegExp("^[A-z]:\\\\(.+?\\\\)*$");
    if(path.test(dir)) return true; else return false;
}

/**
 * 判断字符串是否为空
 * @param s
 * @returns {boolean}
 */
function isNotEmptyStr(s) {
    if (s == null || s === '') {
        return false
    }
    return true
}

/**
 * 字符串对等性判断
 * @param s
 * @param s1
 * @returns {boolean}
 */
function isNotEmptyAndEqualsStr(s,s1) {
    if (typeof s == 'string' && s.length > 0 &&s===s1) {
        return true
    }
    return false
}



// 获取鼠标在html中的绝对位置
function mouseCoords(event){
    if(event.pageX || event.pageY){
        return {x:event.pageX, y:event.pageY};
    }
    return{
        x:event.clientX + document.body.scrollLeft - document.body.clientLeft,
        y:event.clientY + document.body.scrollTop - document.body.clientTop
    };
}
// 获取鼠标在控件的相对位置
function getXAndY(control, event){
    //鼠标点击的绝对位置
    let Ev = event || window.event;
    var mousePos = mouseCoords(event);
    var x = mousePos.x;
    var y = mousePos.y;
    //alert("鼠标点击的绝对位置坐标："+x+","+y);

    //获取div在body中的绝对位置
    var x1 = control.offsetLeft;
    var y1 = control.offsetTop;

    //鼠标点击位置相对于div的坐标
    var x2 = x - x1;
    var y2 = y - y1;
    return {x:x2,y:y2};
}

function sendTouchEvent(minitouchStr) {
    net.send("M_TOUCH://" + minitouchStr);
}

function sendKeyEvent(keyevent) {
    net.send("M_KEYEVENT://" + keyevent)
}

function sendDown(argx, argy, isRo) {
    var scalex = deviceInfo.physicsSize.w / canvas.width;
    var scaley = deviceInfo.physicsSize.h / canvas.height;
    var x = argx, y = argy;
    if (isRo) {
        x = (canvas.height - argy) * (canvas.width / canvas.height);
        y = argx * (canvas.height / canvas.width);
    }
    x = Math.round(x * scalex);
    y = Math.round(y * scaley);
    var command = "d 0 " + x + " " + y + " 50\n";
    command += "c\n";
    sendTouchEvent(command);
}

function sendMove(argx, argy, isRo) {
    var scalex = deviceInfo.physicsSize.w / canvas.width;
    var scaley = deviceInfo.physicsSize.h / canvas.height;
    var x = argx, y = argy;
    if (isRo) {
        x = (canvas.height - argy) * (canvas.width / canvas.height);
        y = argx * (canvas.height / canvas.width);
    }
    x = Math.round(x * scalex);
    y = Math.round(y * scaley);

    var command = "m 0 " + x + " " + y + " 50\n";
    command += "c\n";
    sendTouchEvent(command);
}

function sendUp() {
    var command = "u 0\n";
    command += "c\n";
    sendTouchEvent(command);
}

canvas.onmousedown = function (event) {
    isDown = true;
    var pos = getXAndY(canvas, event);
    sendDown(pos.x, pos.y, deviceWindow.rotate);
};

canvas.onmousemove = function (event) {
    if (!isDown) {
        return;
    }
    var pos = getXAndY(canvas, event);

    sendMove(pos.x, pos.y, deviceWindow.rotate);
};

canvas.onmouseover = function (event) {
    console.log("onmouseover");
};

canvas.onmouseout = function (event) {
    if (!isDown) {
        return;
    }
    isDown = false;
    sendUp();
};

canvas.onmouseup = function (event) {
    if (!isDown) {
        return;
    }
    isDown = false;
    sendUp();
};
