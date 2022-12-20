/*
 * Copyright 2010-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.lhfei.hazelcast.component.cdc;

import java.io.Serializable;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @version 1.4.0
 *
 * @author Hefei Li
 *
 * @created Sep 23, 2022
 */
public class Student implements Serializable {
  
  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAddr() {
    return addr;
  }

  public void setAddr(String addr) {
    this.addr = addr;
  }

  public String getYear() {
    return year;
  }

  public void setYear(String year) {
    this.year = year;
  }

  public String getGrade() {
    return grade;
  }

  public void setGrade(String grade) {
    this.grade = grade;
  }

  public double getChinese() {
    return chinese;
  }

  public void setChinese(double chinese) {
    this.chinese = chinese;
  }

  public double getMath() {
    return math;
  }

  public void setMath(double math) {
    this.math = math;
  }

  public double getEnglish() {
    return english;
  }

  public void setEnglish(double english) {
    this.english = english;
  }
  
  @Override
  public int hashCode() {
      return Objects.hash(name, id);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    Student other = (Student) obj;
    return id == other.id && Objects.equals(name, other.name) && Objects.equals(year, other.year);
  }

  @Override
  public String toString() {
    return "Student {id=" + id + ", name=" + name + ", year=" + year + ", addr=" + addr + ", math="
        + math + ", chinese=" + chinese + ", english=" + english + "}";
  }



  @JsonProperty("id")
  private long id;
  
  @JsonProperty("name")
  private String name;
  
  @JsonProperty("addr")
  private String addr;
  
  @JsonProperty("year")
  private String year;
  
  @JsonProperty("grade")
  private String grade;
  
  @JsonProperty("chinese")
  private double chinese;
  
  @JsonProperty("math")
  private double math;
  
  @JsonProperty("english")
  private double english;
}
