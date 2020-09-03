/*********************************************************
 Copyright (C),2011-2017,Electronic Technology Co.,Ltd.
 File name: 		serial.c
 Author: 			Tangxl
 Version: 			1.0
 Date: 				2014-6-16
 Description: 		
 History: 			
 					
   1.Date:	 		2014-6-16
 	 Author:	 	Tangxl
 	 Modification:  Created file
 	 
*********************************************************/

#include <android/log.h>
#include <jni.h>
#include <string>
#include <sstream>     // std::stringstream
#include <vector>


#include <unistd.h>
#include <stdio.h>
#include <termios.h>
#include <fcntl.h>
#include <string.h>
#include <time.h>


const static char* DTAG = "TemperTest";

#define  LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,DTAG,__VA_ARGS__)

typedef struct ModuleOps
{
	char device[11] ;
	int Fd ;
}ModuleOps;

static ModuleOps moduleOps;

static int speed_arr[] = { B115200, B57600, B38400, B19200, B9600, B4800, B2400, B1200, B300};

static int name_arr[] = {115200, 57600, 38400, 19200, 9600, 4800, 2400, 1200, 300};






/**
 * #purpose	: 字符转十六进制
 * #note	: 不适用于汉字字符
 * #param ch    : 要转换成十六进制的字符
 * #return	: 接收转换后的字符串
 */
std::string chToHex(unsigned char ch)
{
	const std::string hex = "0123456789ABCDEF";

	std::stringstream ss;
	ss << hex[ch >> 4] << hex[ch & 0xf];

	return ss.str();
}

/**
 * #purpose	: 字符串转十六进制字符串
 * #note	: 可用于汉字字符串
 * #param str		: 要转换成十六进制的字符串
 * #param separator	: 十六进制字符串间的分隔符
 * #return	: 接收转换后的字符串
 */
std::string strToHex(std::string str, std::string separator = "")
{
	const std::string hex = "0123456789ABCDEF";
	std::stringstream ss;

	for (std::string::size_type i = 0; i < str.size(); ++i)
		ss << hex[(unsigned char)str[i] >> 4] << hex[(unsigned char)str[i] & 0xf] << separator;

	return ss.str();
}



/*************************************************
 Function:		set_hw_ctsrts
 Descroption:	 
 Input: 
	1.fd
	2.on
 Output: 
 Return: 	
 Other:  
*************************************************/
static int set_hw_ctsrts(int fd ,int on)
{
	struct termios opt;

	if(tcgetattr(fd,&opt) != 0)					        //或许原先的配置信息
	{
		return -1;
	}

    if(on)
    {
	    opt.c_cflag |= CRTSCTS;
    }
    else
    {
        opt.c_oflag &= ~CRTSCTS;
    }

	tcflush(fd,TCIFLUSH);							    //清空输入缓存

	if(tcsetattr(fd,TCSANOW,&opt) != 0)
	{

		return -1;
	}

	return 0;
}

/*************************************************
 Function:		set_speed
 Descroption:	设置fd表述符的串口波特率
 Input: 
	1.fd
	2.speed
 Output: 
 Return: 	    0:succ; other:false
 Other:  
*************************************************/
static int set_speed(int fd ,int speed)
{
	struct termios opt;
	int i;
	int status;
    int ret = -1;

	tcgetattr(fd,&opt);
	for(i = 0;i < (int)(sizeof(speed_arr)/sizeof(int)); i++)
	{
		if(speed == name_arr[i])						//找到标准的波特率与用户一致
		{
			tcflush(fd,TCIOFLUSH);						//清除IO输入和输出缓存
			cfsetispeed(&opt,speed_arr[i]);		    	//设置串口输入波特率
			cfsetospeed(&opt,speed_arr[i]);			    //设置串口输出波特率

			status = tcsetattr(fd,TCSANOW,&opt);	    //将属性设置到opt的数据结构中，并且立即生效
			if(status != 0)
            {         
				LOGD("tcsetattr fd: [%d] [%d]",fd,speed);				        //设置失败
            }
			else
            {    
                ret = 0 ;
            }
            break;
		}
	}

    return ret;
}

/*************************************************
 Function:		set_parity
 Descroption:	设置fd表述符的奇偶校验
 Input: 
	1.fd
 Output: 
 Return: 	    0:succ; other:false
 Other:  
*************************************************/
static int set_parity(int fd, int databits,int stopbits,int parity)
{
	struct termios opt;

//	LOGD("set parity begin");

	if (tcgetattr(fd, &opt) != 0)					        //或许原先的配置信息
	{
		LOGD("Get opt in parity error:");
		return -1;
	}

    #if 0
    /*通过设置opt数据结构，来配置相关功能，以下为八个数据位，不使能奇偶校验*/
	opt.c_iflag &= ~(IGNBRK | BRKINT | PARMRK | ISTRIP
				| INLCR | IGNCR | ICRNL | IXON);
	opt.c_oflag &= ~OPOST;
	opt.c_lflag &= ~(ECHO | ECHONL | ICANON | ISIG | IEXTEN);
	opt.c_cflag &= ~(CSIZE | PARENB);
	opt.c_cflag |= CS8;

    #else
	/*通过设置opt数据结构，来配置相关功能，以下为八个数据位，不使能奇偶校验*/
	opt.c_iflag &= ~(IGNBRK | BRKINT | PARMRK | ISTRIP
				| INLCR | IGNCR | ICRNL | IXON);
	opt.c_oflag &= ~OPOST;
	opt.c_lflag &= ~(ECHO | ECHONL | ICANON | ISIG | IEXTEN);

	// set data bit
	opt.c_cflag &= ~CSIZE;
	switch (databits)
	{
		case 5:
			opt.c_cflag |= CS5;
			break;
		case 6:
			opt.c_cflag |= CS6;
			break;
		case 7:		
			opt.c_cflag |= CS7; 
			break;
		case 8:     
			opt.c_cflag |= CS8;
			break;   
		default:    
			return -1;
	}
//	LOGD("set databits end");

	// set stop bit
	switch (stopbits)
	{
		case 1:    
			opt.c_cflag &= ~CSTOPB;  
			break;  
		case 2:    
			opt.c_cflag |= CSTOPB;  
			break;
		default:    
			break;
	}

	// set parity
	switch (parity)
	{
		//no parity
		case 'n':
		case 'N':
			opt.c_cflag &= ~PARENB;
			opt.c_iflag &= ~INPCK;
			break;
		//ODD parity
		case 'o':   
		case 'O':     
			opt.c_cflag |= PARENB;
			opt.c_cflag |= PARODD;
			opt.c_iflag |= INPCK;
			break;
		//EVEN parity
		case 'e':  
		case 'E':   
			opt.c_cflag |= PARENB;
			opt.c_cflag &= ~PARODD;
			opt.c_iflag |= INPCK;
			break;
		//space parity
		case 'S': 
		case 's':
			opt.c_cflag |= (PARENB | CMSPAR |PARODD);
			//term.c_cflag &= ~PARODD;
			opt.c_iflag |= INPCK;
			break;
		//mark parity
		case 'M':
		case 'm':
			opt.c_cflag |= (PARENB | CMSPAR);
			opt.c_cflag &= ~PARODD;
			opt.c_iflag |= INPCK;
			break;
		default:
			return -1;
	}
    #endif
//    LOGD("set parity end switch");
	tcflush(fd, TCIFLUSH);							    //清空输入缓存

	if (tcsetattr(fd, TCSANOW, &opt) != 0)
	{
		LOGD("set attr parity error:");
		return -1;
	}

	return 0;
}

/*************************************************
 Function:		module_serial_open
 Descroption:	 
 Input: 
	1.ModuleOps * Ops
	2.Param
 Output: 
 Return: 	
 Other:  
*************************************************/
int module_serial_open(struct ModuleOps * Ops, void* Param)
{
	int flag;
    int speed = 9600;
    int is_block = 0;
    int databits = 8;
    int stopbits = 1;
    int parity = 'n';
    int* param = (int*)Param;
    if(Param == NULL || NULL == Ops)
    {
        return -1;
    }
   
    if(Ops->Fd > 0)
    {
        return 0;
    }
    speed = *param;
    is_block = *(param+1);
    databits = *(param+2);
    stopbits = *(param+3);
    parity = *(param+4);
    
	flag = 0;
	flag |= O_RDWR;


//	LOGD("databits is %d" , databits);
//	LOGD("device is %c" , Ops->device[0]);
//	LOGD("speed is %d",speed);
	if(is_block == 0)
	{
	    flag |= O_NONBLOCK;				   
    }

#pragma clang diagnostic push
#pragma ide diagnostic ignored "err_ovl_ambiguous_call"
	Ops->Fd = open(Ops->device,flag);
#pragma clang diagnostic pop

	LOGD("FD after open is %d",Ops->Fd);

	if(Ops->Fd < 0)
	{
		LOGD("Open device file err:");
		close(Ops->Fd);
		return -1;
	}

	set_speed(Ops->Fd,speed);

	if(set_parity(Ops->Fd, databits,stopbits,parity) != 0)
	{
		LOGD("set parity error:");
		close(Ops->Fd);						        
		return -1;
	}

	return Ops->Fd;
}

/*************************************************
 Function:		module_serial_close
 Descroption:	 
 Input: 
	1.ModuleOps * Ops
	2.Param
 Output: 
 Return: 	
 Other:  
*************************************************/
int module_serial_close(struct ModuleOps * Ops)
{
	if(NULL == Ops)
    {
        return -1;
    }

    if(Ops->Fd)
    {
       close(Ops->Fd);
       Ops->Fd = -1;
    }
    
    return 0;
}

/*************************************************
 Function:		module_serial_write
 Descroption:	 
 Input: 
	1.Ops
	2.*str
	3.int len
 Output: 
 Return: 	
 Other:  
*************************************************/
int module_serial_write(struct ModuleOps * Ops, unsigned char *str, int len)
//int module_serial_write(struct ModuleOps * Ops, int str[], int len)

{
//	LOGD("serial write begin");
	int ret = -1;
    
    if(NULL == Ops)
    {
        return -1;
    }

    tcflush(Ops->Fd, TCIFLUSH) ;
    
	ret = write(Ops->Fd,str,len);
	if(ret < 0)
	{
		LOGD("serial send err ");
		return -1;
	}
	return ret;
}

/*************************************************
 Function:		module_serial_read
 Descroption:	 
 Input: 
	1.ModuleOps * Ops
	2.*str
	3.int len
	4.int mtimeout
 Output: 
 Return: 	
 Other:  
*************************************************/
int module_serial_read(struct ModuleOps * Ops, unsigned char* str, int len, unsigned int mtimeout)
{

//	LOGD("serial read begin");
	fd_set rfds;
	struct timeval tv;	
	int sret = -1;	
    int readlen = 0;
	int sec = mtimeout / 1000;
    int usec = (mtimeout%1000)*1000;
    
    if(NULL == Ops)
    {
        return 0;
    }
//	LOGD("serial read ops");

    FD_ZERO(&rfds);				
	FD_SET(Ops->Fd,&rfds);		

    tv.tv_sec  = sec;
    tv.tv_usec = usec;
	sret = select(Ops->Fd+1,&rfds,NULL,NULL,&tv);
	LOGD("the sret is %d",sret);
	if(sret == -1)
	{
		LOGD("select error");
	}
    else if(sret > 0)
	{
		LOGD("sret >0 ");
	    if (FD_ISSET(Ops->Fd, &rfds))
        {
			LOGD("serial read readlen");
            readlen = read(Ops->Fd,  str, len);
        }
	}
//	LOGD("serial read end");

    return readlen;
}



/*************************************************
 Function:		module_serial_init
 Descroption:	 
 Input: 
	1.*Ops
 Output: 
 Return: 	
 Other:  
*************************************************/
int module_serial_init(ModuleOps *Ops)
{
    if(NULL == Ops)
    {
        return -1;
    }

    memset(Ops, 0, sizeof(ModuleOps));
//    sprintf(Ops->name, "%s", "Serial Driver");
	sprintf(Ops->device, "%s", "/dev/ttyS3");
    Ops->Fd = -1;

    return 0;
}






/**
 * 矫正温度
 * @param Tambeint
 * @param Tforehead
 * @return
 */
double mlx90614_compensate_temp(double Tambeint, double Tforehead)
{
	double Tflow;
	double Tfhigh;
	double Tlow;
	double Thigh;
	double Tnormal;
	double Tbody;


	if (Tambeint <= 25) {
		Tflow = 32.66 + 0.186 * (Tambeint - 25);
		Tfhigh = 34.84 + 0.148 * (Tambeint - 25);
	} else {
		Tflow = 32.66 + 0.0864000000000001 * (Tambeint - 25);
		Tfhigh = 34.84 + 0.0999999999999997 * (Tambeint - 25);
	}

	Tlow = 36.3 + (0.551658272522697 + 0.0215250684640259 * Tambeint) * (Tforehead - Tflow);
	Thigh = 36.8 + (0.829320617815896 + 0.0023644335442161 * Tambeint) * (Tforehead - Tfhigh);
	Tnormal = 36.3 + 0.5 / (Tfhigh - Tflow) * (Tforehead - Tflow);

	if (Tforehead < Tflow) {
		Tbody = Tlow;
	} else {
		if (Tforehead > Tfhigh) {
			Tbody = Thigh;
		} else {
			Tbody = Tnormal;
		}
	}

	return Tbody;
}



extern "C" {


JNIEXPORT void Java_com_nlscan_android_tempertest_TemperModel_initSerial(JNIEnv* env, jobject thiz){

	ModuleOps * opsPointer = &moduleOps;
	module_serial_init(opsPointer);

	int speed [5]= {115200,0,8,1,'n'};
	module_serial_open(opsPointer,speed);


}

JNIEXPORT void Java_com_nlscan_android_tempertest_TemperModel_closeSerial(JNIEnv* env, jobject thiz){

	ModuleOps * opsPointer = &moduleOps;

	module_serial_close(opsPointer);
}

// public native boolean Init(AssetManager mgr);
JNIEXPORT jdoubleArray JNICALL Java_com_nlscan_android_tempertest_TemperModel_currentTemper(JNIEnv* env, jobject thiz)
{

	int Tambeint =0;
	int Tforehead = 0;
	ModuleOps * opsPointer = &moduleOps;

//	set_hw_ctsrts(opsPointer->Fd,true);

//	int speed [5]= {115200,0,8,1,'n'};
//	module_serial_open(opsPointer,speed);

	LOGD("the file FD is %d",opsPointer->Fd);

//	unsigned char * input = (unsigned char *)  strToHex("tempreq\0\n","").c_str  ;

//	unsigned char input[] = {74656d7072657100,0x0a};
	unsigned char input[] = {0x74,0x65,0x6d,0x70,0x72,0x65,0x71,0x00,0x0a};

	int size = sizeof(input);
//	int size = sizeof(inputData);

	int writeRel = module_serial_write(opsPointer,input,size);
	LOGD("the write result is %d" ,writeRel);



//	unsigned  char*  temperStr;
	unsigned  char*  temperStr = new unsigned char[size];
	int readRel = module_serial_read(opsPointer,temperStr, size,200);

//	module_serial_close(opsPointer);

	LOGD("the read result is %d" ,readRel);

	LOGD("the read result  str is %x %x %x %x" ,temperStr[3],temperStr[4],temperStr[5],temperStr[6]);

	unsigned char * taPoint = (unsigned char*) &Tambeint;
	unsigned char * toPoint = (unsigned char*) &Tforehead;

	taPoint[0] = temperStr[3];
	taPoint[1] = temperStr[4];

	toPoint[0] = temperStr[5];
	toPoint[1] = temperStr[6];


	double realTambeint = (double)Tambeint /50.0 -273.15;
	double realTforehead = (double)Tforehead /50.0 -273.15 ;

//    double  realTforehead = 36.5;

	LOGD("tambeint is %lf  , tforehead is %lf",realTambeint,realTforehead);

	double resultTemp = 30.00;

//    if ( fabs(realTforehead - realTambeint) >= 2.0 &&  realTforehead >=30.2 && realTforehead <39.0 ){
//
//        if (realTforehead >36.0 || realTforehead <32.0)
//            realTforehead =  realTforehead - round(realTforehead - 34.0);
//
//    }


    resultTemp =   mlx90614_compensate_temp(realTambeint,realTforehead);




//    LOGD("tambeint is %lf  , tforehead is %lf",realTambeint,realTforehead);


	LOGD("resultTemp is %lf",resultTemp);

	delete temperStr;
	temperStr = NULL;




	jdouble result [] = {realTambeint,realTforehead,resultTemp};




	jdoubleArray  args  = (env)->NewDoubleArray(3);

	env->SetDoubleArrayRegion(args,0,3,result);


    return args ;
}



}






