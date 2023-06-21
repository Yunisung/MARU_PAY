// maxlength 강제 적용
function checkMaxLength(object) {
    if (object.value.length > object.maxLength) {
        object.value = object
            .value
            .slice(0, object.maxLength);
    }
}

// 화면 세로 스크롤 조정
function setScrollTop($obj) {

    const $popup = $('.popup-layer');
    const offset = $obj.offset();
    let offsetTop = 0;
    if (offset) {
        offsetTop = offset.top;
    }

    const scrolltop = $popup
        .find('.layer-container')
        .scrollTop();
    $popup
        .find('.layer-container')
        .animate({
            scrollTop: (offsetTop + scrolltop - ($(window).height() / 2))
        }, 300);
}

// 화면 리사이징
function contentResize() {

    const $popup = $('.popup-layer');
    // height 조정 const containerheights = $popup.find(".layer-container").height();
    const popupHeights = $popup.height();
    const headerHeights = $popup
        .find(".header-title")
        .outerHeight(true);
    const navHeights = $popup
        .find(".nav")
        .outerHeight(true);
    const footerHeights = $popup
        .find("#footer")
        .outerHeight(true);

    $popup
        .find('.layer-container')
        .css('height', (popupHeights - footerHeights) + "px");
    $popup
        .find('.content')
        .css(
            'height',
            (popupHeights - headerHeights - navHeights - footerHeights) + "px"
        );

}

/**
 * 약관 동의 styling
 */
function provisionStyling() {

    let i = 1;
    if (!KWON.util.isMobile() || ($(window).width() >= 960)) {
        $("#provision-area").addClass("agree-area");
    }
    $("span.detail").each(function () {
        let anchor = null;
        /*if (KWON.util.isMobile()) {
            anchor = $(document.createElement('a')).prop({
                target: '_blank',
                // target: '#',
                href: "/form/payment/auth/step_01_provision" + i + ".html",
                innerText: '약관보기'
            })
        } else {*/
            anchor = $(document.createElement('a')).prop({href: '#', innerText: '약관보기'});
        // }
        $(this).append(anchor);
        i = i + 1;
    });

}

/** 로딩바 */
var loading = {

    // 시작
    start: function () {

        let loading = document.getElementById('loading');
        if (loading === undefined || loading === null) {
            this.createLoadingBar();
        } else {
            loading.style.display = 'flex';
        }
    },

    // 종료
    end: function () {
        let loading = document.getElementById('loading');
        loading.style.display = 'none';
    },

    // 로딩바 생성
    createLoadingBar: function () {

        let img = document.createElement('img');
        img.setAttribute("src", "/static/img/auth/loading.svg");
        img.setAttribute("width", "17%");
        img.setAttribute("height", "17%");

        let loading = document.createElement('div');
        loading.setAttribute("id", "loading");
        loading.appendChild(img);

        let layer = document.getElementsByClassName('popup-layer')[0];
        layer.appendChild(loading);
    }
}

window.onload = function () {
    /** 화면 사이즈 */
    let vh = window.innerHeight * 0.01;
    document
        .documentElement
        .style
        .setProperty("--vh", vh + 'px');

    //ie11
    var agent = navigator
        .userAgent
        .toLowerCase();
    if ((navigator.appName == 'Netscape' && agent.indexOf('trident') != -1) || (agent.indexOf("msie") != -1)) {
        $('.popup-layer').css('height', (vh * 100) + 'px');
    }

    window.addEventListener("resize", function () {

        vh = window.innerHeight * 0.01;
        document
            .documentElement
            .style
            .setProperty("--vh", vh + 'px');

        contentResize();

        setScrollTop($(':focus'));

    });

    window.onpageshow = function (event) {

        if ((window.performance && window.performance.navigation.type == 2)
                && (window.history.state != null && window.history.state.hasOwnProperty('prevUrl'))) {
            var result = KWON.okBtnExit();
            if(!result) {
                history.go(1);
            }
        } else {
            
            history.pushState({prevUrl: window.location.href}, null, location.href);
            window.onpopstate = function () {
                history.go(1);
            };
        }

    }
}

$(document).ready(function () {

    contentResize();
    provisionStyling();

    if ($('#scrollable').length) {

        const contentheights = $popup
            .find(".content")
            .height();
        const titleHeights = $popup
            .find("#content-title")
            .outerHeight(true);
        const mainHeights = $popup
            .find(".main")
            .outerHeight(true);
        const errorHeights = $popup
            .find(".error_message")
            .outerHeight(true);
        $('#scrollable').css(
            'height',
            (contentheights - titleHeights - mainHeights - errorHeights + 2) + "px"
        );
    }

    $("input").on("focus", function () {

        setScrollTop($(this));

        return false;

    });

    // input 자동 다음 input으로
    $("input.next").on("keyup", function () {
        const charLimit = $(this).attr("maxlength");
        if (this.value.length >= charLimit) {
            $(this)
                .next('input')
                .focus();
            return false;
        }
    });

    // 마지막 input은 확인 버튼 영역 focus
    $("input.last").on("keyup", function () {
        const charLimit = $(this).attr("maxlength");
        if (this.value.length >= charLimit) {
            $('#footer')
                .find('button.next')
                .focus();
            return false;
        }
    });
});
