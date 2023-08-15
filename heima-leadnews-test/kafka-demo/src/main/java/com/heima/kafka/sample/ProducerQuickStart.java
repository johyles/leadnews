package com.heima.kafka.sample;

import org.apache.kafka.clients.producer.*;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

/**
 * 生产者
 */
public class ProducerQuickStart {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        //1.kafka的配置信息
        Properties prop = new Properties();
        //kafka的连接地址
        prop.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"192.168.200.130:9092");
        //消息key的序列化器
        prop.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");
        //消息value的序列化器
        prop.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");

        //ACK配置 消息确认机制
        prop.put(ProducerConfig.ACKS_CONFIG,"all");

        //重发次数
        prop.put(ProducerConfig.RETRIES_CONFIG,10);

        //数据压缩
        prop.put(ProducerConfig.COMPRESSION_TYPE_CONFIG,"snappy");

        //2.生产者对象
        KafkaProducer<String,String> producer = new KafkaProducer<String, String>(prop);

        //3.封装发送的消息
        /**
         * 1.topic
         * 2.key
         * 3.value
         */
        for (int i=0;i<5;i++){
            ProducerRecord<String,String> kvProducerRecord = new ProducerRecord<String, String>("iscast-topic-input","hello kafka");
            producer.send(kvProducerRecord);
        }

//        //同步发送消息
//        RecordMetadata recordMetadata = producer.send(kvProducerRecord).get();
//        System.out.println(recordMetadata.offset());
        //异步消息发送
        /*producer.send(kvProducerRecord, new Callback() {
            @Override
            public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                if(e != null){
                    System.out.println("记录异常信息到日志表中");
                }
                System.out.println(recordMetadata.offset());
            }
        });*/

        //4.关闭消息通道，必须关闭，否则消息发送不成功
        producer.close();
    }

}