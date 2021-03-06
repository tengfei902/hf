<!DOCTYPE html>
<html lang="zh-CN">
<%@ page contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<head> 
<meta http-equiv="Content-Type"	content="text/html; charset=utf-8" />
<link rel="stylesheet" href="http://cdn.bootcss.com/bootstrap/3.3.5/css/bootstrap.min.css">
<link rel="stylesheet" href="http://cdn.bootcss.com/bootstrap/3.3.5/css/bootstrap-theme.min.css">
<script src="http://cdn.bootcss.com/jquery/1.11.3/jquery.min.js"></script>
<script src="http://cdn.bootcss.com/bootstrap/3.3.5/js/bootstrap.min.js"></script>
<script src="http://huifufu.cn/Public/Front/js/md5.js"></script>
<title>慧富支付-商户demo</title>
<style type="text/css">
	body { padding-top: 70px; }
</style>
</head> 
<body> 
<nav class="navbar navbar-default navbar-fixed-top" role="navigation">
  <div class="container">
  	<a class="navbar-brand"><strong>慧富支付demo（<s>ie1-9</s>）</strong></a>
  </div>
</nav>

<div class="container-fluid">
	<div class="row">
		<div class="col-md-6 col-md-offset-3">
			<form role="form" action="http://huifufu.cn/openapi/unifiedorder_2" method="post"  name="payForm" >
				<div class="form-group">
					<label for="version">版本号</label>
					<input type="text" class="form-control" name="version" id="version" value="2.0" required>
				</div>
				<div class="form-group">
					<label for="merchant_no">商户号</label>
					<input type="text" class="form-control" name="merchant_no" id="merchant_no" value="" required>
				</div>
				<div class="form-group">
					<label for="service">支付类型</label>
					<select name="service" id="service">
						<option value="">选择支付类型</option>
						<option value="01">微信公众号</option>
						<option value="02">微信扫码</option>
						<option value="03">微信反扫</option>
						<option value="04">微信H5</option>
						<option value="05">微信APP</option>
						<option value="06">支付宝服务窗体</option>
						<option value="07">支付宝扫码</option>
						<option value="08">支付宝反扫</option>
						<option value="09">网银</option>
						<option value="10">QQ H5</option>
						<option value="11">QQ扫码</option>
						<option value="12">银联扫码</option>
						<option value="13">快捷支付</option>
					</select>
				</div>
				<div class="form-group">
					<label for="total">支付金额</label>
					<input type="text" class="form-control" name="total" id="total" value="" required>
				</div>
				<div class="form-group">
					<label for="name">订单名称</label>
					<input type="text" class="form-control" name="name" id="name" value="" required>
				</div>
				<div class="form-group">
					<label for="remark">订单备注</label>
					<input type="text" class="form-control" name="remark" id="remark" value="" required>
				</div>
				<div class="form-group">
					<label for="out_trade_no">订单号</label>
					<input type="text" class="form-control" name="out_trade_no" id="out_trade_no" value="" required>
				</div>
				<div class="form-group">
					<label for="out_notify_url">后台通知地址</label>
					<input type="text" class="form-control" name="out_notify_url" id="out_notify_url" value="" required>
				</div>
				<div class="form-group">
					<label for="front_url">前台跳转地址</label>
					<input type="text" class="form-control" name="front_url" id="front_url" value="" required>
				</div>
				<div class="form-group">
					<label for="create_ip">ip</label>
					<input type="text" class="form-control" name="create_ip" id="create_ip" value="" required>
				</div>
				<div class="form-group">
					<label for="buyer_id">付款人id</label>
					<input type="text" class="form-control" name="buyer_id" id="buyer_id" value="" required>
				</div>
				<div class="form-group">
					<label for="bank_code">银行</label>
					<select name="bank_code" id="bank_code">
						<option value="">选择支付银行</option>
						<option value="ICBC">中国工商银行</option>
						<option value="ABC">中国农业银行</option>
						<option value="BOC">中国银行</option>
						<option value="CCB">中国建设银行</option>
						<option value="BOCOM">交通银行</option>
						<option value="CMB">招商银行</option>
						<option value="CEB">光大银行</option>
						<option value="CMBC">民生银行</option>
						<option value="PSBC">中国邮政储蓄银行</option>
						<option value="SPDB">浦发银行</option>
						<option value="CNCB">中信银行</option>
						<option value="PAB">平安银行</option>
						<option value="HXB">华夏银行</option>
						<option value="CIB">兴业银行</option>
						<option value="BOHC">渤海银行</option>
						<option value="BCCB">北京银行</option>
						<option value="GDB">广发银行</option>
						<option value="BOS">上海银行</option>
						<option value="ZSBC">浙商银行</option>
						<option value="NBBC">宁波银行</option>
						<option value="NJBC">南京银行</option>
						<option value="BRCB">北京农村商业银行</option>
						<option value="ZHTLCB">浙江泰隆商业银行</option>
						<option value="BEA">东亚银行</option>
						<option value="HZB">杭州银行</option>
					</select>
				</div>
				<div class="form-group">
					<label for="nonce_str">随机字符串</label>
					<input type="text" class="form-control" name="nonce_str" id="nonce_str" value="" required>
				</div>
				<div class="form-group">
					<label for="sign_type">加密方式</label>
					<input type="text" class="form-control" name="sign_type" id="sign_type" value="MD5" required>
				</div>
				<div class="form-group">
					<label for="cipherCode">密钥</label>
					<input type="text" class="form-control" name="cipherCode" id="cipherCode" value="" required>
				</div>
				<input type="hidden" name="sign" id="sign">
				<button type="button" onclick="beforeSubmit()" class="btn btn-primary">提交</button>
			</form>
		</div>
	</div>
</div>
</body>

<script>
    beforeSubmit = function(){
		var cipherCode = $("#cipherCode").val();

		var arr = ["version","merchant_no","service","total","name","remark","out_trade_no","out_notify_url",
            "front_url","create_ip","buyer_id","bank_code","nonce_str","sign_type"];
        arr.sort();

        var encryptStr = "";

        for(var i=0;i<arr.length;i++) {
            var key = arr[i];
            var data = $("#"+arr[i]).val() ;
			if(isNull(data)) {
			    continue;
			}
            encryptStr = encryptStr+key+"="+data+"&";
		}
		encryptStr = encryptStr+"key="+cipherCode;
        var sign = $.md5(encryptStr);

        $("#cipherCode").remove();
        $("#sign").attr("value",sign.toUpperCase());
        $("form").submit();
    }

    function isNull( str ){
        if(str==null) return true;
        if ( str == "" ) return true;
        var regu = "^[ ]+$";
        var re = new RegExp(regu);
        return re.test(str);
    }

</script>
 
</html>