<!--商品详情-->
<template>
  <div class="w store-content">
    <div class="gray-box">
      <div class="gallery-wrapper">
        <div class="gallery">
          <div class="thumbnail">
            <ul>
              <li v-for="(item,i) in small" :key="i" :class="{on:big===item}" @click="big=item">
                <img v-lazy="item" :alt="product.productName">
              </li>
            </ul>
          </div>
          <div class="thumb">
            <div class="big">
              <img :src="big" :alt="product.productName">
            </div>
          </div>
        </div>
      </div>
      <!--右边-->
      <div class="banner">
        <div class="sku-custom-title">
          <h4>{{product.productName}}</h4>
          <h6>
            <span>{{product.subTitle}}</span>
            <span class="price">
              <i>秒杀价  </i><em>¥</em><i>{{product.salePrice.toFixed(2)}}</i></span>
          </h6>
        </div>
        <div class="num">
          <span class="params-name">数量 : </span>
          <buy-num @edit-num="editNum" :limit="Number(1)"></buy-num>
            &nbsp;&nbsp;&nbsp;<i>秒杀商品每人限购一件</i>
        </div>
        <div class="time">
          <span class="params-name">{{timeDesc}} : {{countDownList}}</span>
          <!-- <buy-num @edit-num="editNum" :limit="Number(product.limitNum)"></buy-num> -->
        </div>
        <div class="time">
          <span class="params-name">活动时间 : {{product.startDate | formatDate}} 至 {{product.endDate | formatDate}}</span>
          <!-- <buy-num @edit-num="editNum" :limit="Number(product.limitNum)"></buy-num> -->
        </div>
        <div class="buy">
          <div class="input">
                 <input type="text" v-model="captcha" placeholder="验证码" v-if="captchaFlag"/>
                 &nbsp;&nbsp;&nbsp;
                 <img id="imageCode" :src="imageCode" @click="init_geetest()" style="width: 100px;height: 40px;line-height: 48px;" v-if="captchaFlag"/>
                  &nbsp;&nbsp;&nbsp;  
          </div>
          <br>
          <y-button 
                  :classStyle="stateText==='立即抢购'||stateText==='请先登录'?'main-btn':'disabled-btn'"
                  :text="stateText"
                  @btnClick="checkout(product.promId)"
                  style="width: 145px;height: 50px;line-height: 48px;text-align:center;vertical-align: middle;">></y-button>
        </div>
      </div>
    </div>
    <!--产品信息-->
    <div class="item-info">
      <y-shelf title="产品信息">
        <div slot="content">
          <div class="img-item" v-if="productMsg">
            <div v-html="productMsg">{{ productMsg }}</div>
          </div>
          <div class="no-info" v-else>
            <img src="/static/images/no-data.png">
            <br>
            该商品暂无内容数据
          </div>
        </div>
      </y-shelf>
    </div>
    <el-dialog title=""
      :visible.sync="lineup">
      <img src="/static/images/lineup.jpg" width="100%" height="100%" alt="">
    </el-dialog>
    <el-dialog title=""
      :visible.sync="lineupFail">
      <img src="/static/images/lineupFail.jpg" width="100%" height="100%" alt="">
    </el-dialog>
  </div>
</template>
<script>
  import { getRushBuyDetails, addCart } from '/api/goods'
  import { mapMutations, mapState } from 'vuex'
  import { getStrongCaptcha, secKill, querySecKillResult, addSecKillCart, userInfo } from '/api/index.js'
  import {formatDate} from '/api/formatDate.js'
  import YShelf from '/components/shelf'
  import BuyNum from '/components/buynum'
  import YButton from '/components/YButton'
  import { getStore } from '/utils/storage'
  export default {
    filters: {
      formatDate (time) {
        let date = new Date(time)
        console.log(new Date(time))
        return formatDate(date, 'yyyy年MM月dd日 hh:mm')
      }
    },
    data () {
      return {
        productMsg: {},
        small: [],
        big: '',
        product: {
          salePrice: 0
        },
        productNum: 1,
        userId: '',
        captcha: '',
        captchaFlag: false,
        imageCode: '',
        stateText: '立即抢购',
        countDownList: '00天00时00分00秒',
        timeDesc: '距离开始时间',
        lineup: false,
        lineupFail: false,
        skToken: ''
      }
    },
    computed: {
      ...mapState(['login', 'showMoveImg', 'showCart'])
    },
    methods: {
      ...mapMutations(['ADD_CART', 'ADD_ANIMATION', 'SHOW_CART']),
      _getRushBuyDetails (promId) {
        getRushBuyDetails({params: {promId}}).then(res => {
          let result = res.result
          this.product = result
          this.productMsg = result.detail || ''
          this.small = result.productImageSmall
          this.big = this.small[0]
          this.state = result.state
          this.startDate = result.startDate
          this.endDate = result.endDate
          if (result.state === 'ing') {
            this.stateText = '立即抢购'
            this.captchaFlag = true
            this.init_geetest(this.promId)
          } else if (result.state === 'nobegin') {
            this.stateText = '活动未开始'
          } else if (result.state === 'end') {
            this.stateText = '活动已结束'
          } else if (result.state === 'sellout') {
            this.stateText = '已抢光'
          }

          if (result.state === 'ing') {
            this.timeDesc = '距离结束时间'
          }

          let params = {
            params: {
              token: getStore('token')
            }
          }
          userInfo(params).then(res => {
            if (res.result.state !== 1) { // 没登录
              this.stateText = '请先登录'
              this.captchaFlag = false
            }
          })
        })
      },
      addCart (id, price, name, img) {
        if (!this.showMoveImg) {     // 动画是否在运动
          if (this.login) { // 登录了 直接存在用户名下
            addCart({userId: this.userId, productId: id, productNum: this.productNum}).then(res => {
              // 并不重新请求数据
              this.ADD_CART({
                productId: id,
                salePrice: price,
                productName: name,
                productImg: img,
                productNum: this.productNum
              })
            })
          } else { // 未登录 vuex
            this.ADD_CART({
              productId: id,
              salePrice: price,
              productName: name,
              productImg: img,
              productNum: this.productNum
            })
          }
          // 加入购物车动画
          var dom = event.target
          // 获取点击的坐标
          let elLeft = dom.getBoundingClientRect().left + (dom.offsetWidth / 2)
          let elTop = dom.getBoundingClientRect().top + (dom.offsetHeight / 2)
          // 需要触发
          this.ADD_ANIMATION({moveShow: true, elLeft: elLeft, elTop: elTop, img: img})
          if (!this.showCart) {
            this.SHOW_CART({showCart: true})
          }
        }
      },
      checkout (promId) {
         // 发起秒杀排队
        var queryCount = 0
        secKill({promId: promId, captcha: this.captcha}).then(res => {
          if (res.success === false) {
            if (res.code === 1) {
              // 验证码验证失败,弹出异常提示
              this.message(res.message)
            } else {
              // 其他失败查询,查询秒杀资格,但减少轮询次数
              this.lineup = true
              queryCount = 3
            }
          } else {
            this.lineup = true
          }
        })
        setTimeout(() => {
        }, 1000)
        var interval = setInterval(() => {
          querySecKillResult({params: {promId}}).then(res => {
            this.skToken = res.result
            if (this.skToken !== null) {
              clearInterval(interval)
              this.lineup = false
              // 排队成功
              addSecKillCart({promId: promId, token: this.skToken}).then(res => {
                if (res.result === 1) {
                  this.$router.push({path: '/checkoutRushBuy', query: {promId}})
                }
              })
              // alert(this.skToken)
            } else {
              queryCount = queryCount + 1
              if (queryCount === 4) {
                clearInterval(interval)
                // 排队失败
                this.lineup = false
                this.lineupFail = true
              }
            }
          })
        }, 3000)
      },
      editNum (num) {
        this.productNum = num
      },
      init_geetest (extendKey) {
        getStrongCaptcha({params: {extendKey}}).then(res => {
          this.imageCode = 'data:image/gif;base64,' + res.result
        })
      },
      timeFormat (param) {
        return param < 10 ? '0' + param : param
      },
      countDown (it) {
        var interval = setInterval(() => {
          let newTime = new Date().getTime()
          let endTime = new Date(this.endDate).getTime()
          if (this.state === 'ing') {
            endTime = new Date(this.endDate).getTime()
          } else if (this.state === 'nobegin') {
            endTime = new Date(this.startDate).getTime()
          }
          let obj = null
          if (endTime - newTime > 0) {
            let time = (endTime - newTime) / 1000
            let day = parseInt(time / (60 * 60 * 24))
            let hou = parseInt(time % (60 * 60 * 24) / 3600)
            let min = parseInt(time % (60 * 60 * 24) % 3600 / 60)
            let sec = parseInt(time % (60 * 60 * 24) % 3600 % 60)
            obj = {
              day: this.timeFormat(day),
              hou: this.timeFormat(hou),
              min: this.timeFormat(min),
              sec: this.timeFormat(sec)
            }
          } else { // 活动已结束，全部设置为'00'
            obj = {
              day: '00',
              hou: '00',
              min: '00',
              sec: '00'
            }
            let id = this.$route.query.productId
            this._getRushBuyDetails(id)
            clearInterval(interval)
          }
          this.countDownList = obj.day + '天' + obj.hou + '时' + obj.min + '分' + obj.sec + '秒'
        }, 1000)
      },
      message (m) {
        this.$message.error({
          message: m
        })
      }
    },
    mounted () {
      this.userId = getStore('userId')
    },
    components: {
      YShelf, BuyNum, YButton
    },
    created () {
      let id = this.$route.query.promId
      this._getRushBuyDetails(id)
      this.countDown()
    }
  }
</script>
<style lang="scss" scoped>
  @import "../../assets/style/mixin";

  .input {
    height: 50px;
    display: flex;
    align-items: center;
    input {
      font-size: 16px;
      width: 32%;
      height: 85%;
      padding: 10px 15px;
      box-sizing: border-box;
      border: 1px solid #ccc;
      border-radius: 6px;
    }
  }
  .store-content {
    clear: both;
    width: 1220px;
    min-height: 600px;
    padding: 0 0 25px;
    margin: 0 auto;
  }

  .gray-box {
    display: flex;
    padding: 60px;
    margin: 20px 0;
    .gallery-wrapper {
      .gallery {
        display: flex;
        width: 540px;
        .thumbnail {
          li:first-child {
            margin-top: 0px;
          }
          li {
            @include wh(80px);
            margin-top: 10px;
            padding: 12px;
            border: 1px solid #f0f0f0;
            border: 1px solid rgba(0, 0, 0, .06);
            border-radius: 5px;
            cursor: pointer;
            &.on {
              padding: 10px;
              border: 3px solid #ccc;
              border: 3px solid rgba(0, 0, 0, .2);
            }
            img {
              display: block;
              @include wh(100%);
            }
          }
        }
        .thumb {
          .big {
            margin-left: 20px;
          }
          img {
            display: block;
            @include wh(440px)
          }
        }
      }
    }
    // 右边
    .banner {
      width: 450px;
      margin-left: 10px;
      h4 {
        font-size: 24px;
        line-height: 1.25;
        color: #000;
        margin-bottom: 13px;
      }
      h6 {
        font-size: 14px;
        line-height: 1.5;
        color: #bdbdbd;
        display: flex;
        align-items: center;
        justify-content: space-between;
      }
      .sku-custom-title {
        overflow: hidden;
        padding: 8px 8px 18px 10px;
        position: relative;
      }
      .params-name {
        padding-right: 20px;
        font-size: 14px;
        color: #8d8d8d;
        line-height: 36px;
      }
      .num {
        padding: 20px 0 17px 10px;
        border-top: 1px solid #ebebeb;
        display: flex;
        align-items: center;
      }
      .time {
        padding: 20px 0 17px 10px;
        border-top: 1px solid #ebebeb;
        display: flex;
        align-items: center;
      }
      .buy {
        position: relative;
        border-top: 1px solid #ebebeb;
        padding: 12px 0 0 10px;
      }
    }
  }

  .item-info {

    .gray-box {
      padding: 0;
      display: block;
    }
    .img-item {
      width: 1220px;
      // padding: 1vw;
      text-align: center;
      img {
        width: 100%;
        height: auto;
        display: block;
      }
    }
  }

  .no-info {
    padding: 200px 0;
    text-align: center;
    font-size: 30px;
  }

  .price {
    display: block;
    color: #d44d44;
    font-weight: 700;
    font-size: 16px;
    line-height: 20px;
    text-align: right;
    i {
      padding-left: 2px;
      font-size: 24px;
    }
  }
  .lineup{
    position:absolute; 
    left:300px; 
    top:100px; 
    width:400px; 
    height:200px
  }
</style>
